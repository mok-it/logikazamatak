ALTER TABLE ONLY "public"."Teams"
    DROP CONSTRAINT IF EXISTS "Teams_money_non_negative";

ALTER TABLE "public"."Teams"
    DROP COLUMN IF EXISTS "money";

ALTER TABLE "public"."Teams"
    ADD COLUMN IF NOT EXISTS "additionalScoreAwarded" integer NOT NULL DEFAULT 0;

CREATE OR REPLACE FUNCTION "public"."validate_shop_purchase"()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    "item_row" record;
    "task_location_id" bigint;
    "team_score" integer;
    "spent_points" integer;
BEGIN
    IF NEW."teamId" IS NULL THEN
        RAISE EXCEPTION 'Shop purchases require a teamId';
    END IF;

    SELECT
        "items"."id" AS "itemId",
        "items"."gameId" AS "gameId",
        "items"."price" AS "price",
        "items"."maxPerTeam" AS "maxPerTeam",
        "effects"."code" AS "effectCode"
    INTO "item_row"
    FROM "public"."Items" AS "items"
    JOIN "public"."ItemEffects" AS "effects"
      ON "effects"."id" = "items"."itemEffectId"
    WHERE "items"."id" = NEW."itemId";

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Unknown itemId %', NEW."itemId";
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM "public"."Teams" AS "teams"
        JOIN "public"."TeamAssignment" AS "assignment"
          ON "assignment"."id" = "teams"."teamAssignmentId"
        WHERE "teams"."id" = NEW."teamId"
          AND "assignment"."gameId" = "item_row"."gameId"
    ) THEN
        RAISE EXCEPTION 'Team % cannot buy item % outside its game', NEW."teamId", NEW."itemId";
    END IF;

    IF "item_row"."maxPerTeam" IS NOT NULL
       AND (
           SELECT count(*)
           FROM "public"."Shop" AS "shop"
           WHERE "shop"."itemId" = NEW."itemId"
             AND "shop"."teamId" = NEW."teamId"
       ) >= "item_row"."maxPerTeam" THEN
        RAISE EXCEPTION 'Team % already reached the purchase limit for item %', NEW."teamId", NEW."itemId";
    END IF;

    CASE "item_row"."effectCode"
        WHEN 'task_score_multiplier' THEN
            IF NEW."targetId" IS NULL THEN
                RAISE EXCEPTION 'Task score multiplier purchases require a task target';
            END IF;

            SELECT "public"."task_location_id"(NEW."targetId") INTO "task_location_id";

            IF "task_location_id" IS NULL
               OR NOT EXISTS (
                   SELECT 1
                   FROM "public"."Tasks" AS "tasks"
                   WHERE "tasks"."id" = NEW."targetId"
                     AND "tasks"."gameId" = "item_row"."gameId"
               ) THEN
                RAISE EXCEPTION 'Task % is not part of item game %', NEW."targetId", "item_row"."gameId";
            END IF;

            IF "public"."team_has_location_doubler"(NEW."teamId", "task_location_id") THEN
                RAISE EXCEPTION 'Task % is already covered by a location doubler for team %', NEW."targetId", NEW."teamId";
            END IF;

            IF EXISTS (
                SELECT 1
                FROM "public"."TasksLedger" AS "ledger"
                WHERE "ledger"."teamId" = NEW."teamId"
                  AND "ledger"."taskId" = NEW."targetId"
                  AND "ledger"."isSuccess" IS TRUE
                  AND "public"."is_base_task_attempt"("ledger"."bonusSourceShopId")
            ) THEN
                RAISE EXCEPTION 'Task % already has earned score; use the retroactive task multiplier instead', NEW."targetId";
            END IF;

            IF "public"."team_has_task_specific_doubler"(NEW."teamId", NEW."targetId") THEN
                RAISE EXCEPTION 'Task % already has a score multiplier for team %', NEW."targetId", NEW."teamId";
            END IF;

        WHEN 'retroactive_task_score_multiplier' THEN
            IF NEW."targetId" IS NULL THEN
                RAISE EXCEPTION 'Retroactive task score multiplier purchases require a task target';
            END IF;

            SELECT "public"."task_location_id"(NEW."targetId") INTO "task_location_id";

            IF "task_location_id" IS NULL
               OR NOT EXISTS (
                   SELECT 1
                   FROM "public"."Tasks" AS "tasks"
                   WHERE "tasks"."id" = NEW."targetId"
                     AND "tasks"."gameId" = "item_row"."gameId"
               ) THEN
                RAISE EXCEPTION 'Task % is not part of item game %', NEW."targetId", "item_row"."gameId";
            END IF;

            IF "public"."team_has_location_doubler"(NEW."teamId", "task_location_id") THEN
                RAISE EXCEPTION 'Task % is already covered by a location doubler for team %', NEW."targetId", NEW."teamId";
            END IF;

            IF "public"."team_has_task_specific_doubler"(NEW."teamId", NEW."targetId") THEN
                RAISE EXCEPTION 'Task % already has a score multiplier for team %', NEW."targetId", NEW."teamId";
            END IF;

            IF NOT EXISTS (
                SELECT 1
                FROM "public"."TasksLedger" AS "ledger"
                WHERE "ledger"."teamId" = NEW."teamId"
                  AND "ledger"."taskId" = NEW."targetId"
                  AND "ledger"."isSuccess" IS TRUE
                  AND "public"."is_base_task_attempt"("ledger"."bonusSourceShopId")
                  AND NOT EXISTS (
                      SELECT 1
                      FROM "public"."TasksLedger" AS "bonus"
                      WHERE "bonus"."bonusSourceTasksLedgerId" = "ledger"."id"
                  )
            ) THEN
                RAISE EXCEPTION 'Task % has no completed score left to double for team %', NEW."targetId", NEW."teamId";
            END IF;

        WHEN 'retroactive_location_score_multiplier' THEN
            IF NEW."targetId" IS NULL THEN
                RAISE EXCEPTION 'Retroactive location score multiplier purchases require a location target';
            END IF;

            IF NOT EXISTS (
                SELECT 1
                FROM "public"."Locations" AS "locations"
                WHERE "locations"."id" = NEW."targetId"
                  AND "locations"."gameId" = "item_row"."gameId"
            ) THEN
                RAISE EXCEPTION 'Location % is not part of item game %', NEW."targetId", "item_row"."gameId";
            END IF;

            IF "public"."team_has_location_doubler"(NEW."teamId", NEW."targetId") THEN
                RAISE EXCEPTION 'Location % already has a retroactive multiplier for team %', NEW."targetId", NEW."teamId";
            END IF;

            IF EXISTS (
                SELECT 1
                FROM "public"."Tasks" AS "tasks"
                WHERE "tasks"."locationId" = NEW."targetId"
                  AND "public"."team_has_task_specific_doubler"(NEW."teamId", "tasks"."id")
            ) THEN
                RAISE EXCEPTION 'Location % contains a task that already has a task-specific doubler for team %', NEW."targetId", NEW."teamId";
            END IF;

            IF NOT EXISTS (
                SELECT 1
                FROM "public"."TasksLedger" AS "ledger"
                JOIN "public"."Tasks" AS "tasks"
                  ON "tasks"."id" = "ledger"."taskId"
                WHERE "ledger"."teamId" = NEW."teamId"
                  AND "ledger"."isSuccess" IS TRUE
                  AND "public"."is_base_task_attempt"("ledger"."bonusSourceShopId")
                  AND "tasks"."locationId" = NEW."targetId"
                  AND NOT EXISTS (
                      SELECT 1
                      FROM "public"."TasksLedger" AS "bonus"
                      WHERE "bonus"."bonusSourceTasksLedgerId" = "ledger"."id"
                  )
            ) THEN
                RAISE EXCEPTION 'Location % has no completed score left to double for team %', NEW."targetId", NEW."teamId";
            END IF;

        WHEN 'miniboss_unlock' THEN
            IF NEW."targetId" IS NULL THEN
                RAISE EXCEPTION 'Miniboss unlock purchases require a miniboss task target';
            END IF;

            IF NOT EXISTS (
                SELECT 1
                FROM "public"."Tasks" AS "tasks"
                WHERE "tasks"."id" = NEW."targetId"
                  AND "tasks"."gameId" = "item_row"."gameId"
                  AND "tasks"."isMiniBoss" IS TRUE
            ) THEN
                RAISE EXCEPTION 'Task % is not a miniboss in game %', NEW."targetId", "item_row"."gameId";
            END IF;

            IF "public"."team_has_miniboss_unlock"(NEW."teamId", NEW."targetId") THEN
                RAISE EXCEPTION 'Team % already unlocked miniboss task %', NEW."teamId", NEW."targetId";
            END IF;

        WHEN 'miniboss_rewind' THEN
            IF NEW."targetId" IS NULL THEN
                RAISE EXCEPTION 'Miniboss rewind purchases require a miniboss task target';
            END IF;

            IF NOT EXISTS (
                SELECT 1
                FROM "public"."Tasks" AS "tasks"
                WHERE "tasks"."id" = NEW."targetId"
                  AND "tasks"."gameId" = "item_row"."gameId"
                  AND "tasks"."isMiniBoss" IS TRUE
            ) THEN
                RAISE EXCEPTION 'Task % is not a miniboss in game %', NEW."targetId", "item_row"."gameId";
            END IF;

            IF "public"."team_has_miniboss_rewind"(NEW."teamId", NEW."targetId") THEN
                RAISE EXCEPTION 'Team % already owns a miniboss rewind for task %', NEW."teamId", NEW."targetId";
            END IF;

            IF "public"."team_has_miniboss_success"(NEW."teamId", NEW."targetId") THEN
                RAISE EXCEPTION 'Miniboss task % is already defeated by team %', NEW."targetId", NEW."teamId";
            END IF;

            IF NOT "public"."team_has_miniboss_failed_base_attempt"(NEW."teamId", NEW."targetId") THEN
                RAISE EXCEPTION 'Team % must fail miniboss task % before buying a rewind', NEW."teamId", NEW."targetId";
            END IF;

        WHEN 'boss_location_unlock' THEN
            IF NEW."targetId" IS NULL THEN
                RAISE EXCEPTION 'Boss location unlock purchases require a location target';
            END IF;

            IF "item_row"."price" <> 0 THEN
                RAISE EXCEPTION 'Boss location unlock items must cost 0';
            END IF;

            IF NOT EXISTS (
                SELECT 1
                FROM "public"."Locations" AS "locations"
                WHERE "locations"."id" = NEW."targetId"
                  AND "locations"."gameId" = "item_row"."gameId"
            ) THEN
                RAISE EXCEPTION 'Location % is not part of item game %', NEW."targetId", "item_row"."gameId";
            END IF;

            IF "public"."team_has_effect_purchase"(NEW."teamId", NEW."targetId", 'boss_location_unlock') THEN
                RAISE EXCEPTION 'Team % already unlocked boss location %', NEW."teamId", NEW."targetId";
            END IF;

            IF NOT "public"."team_defeated_all_minibosses"(NEW."teamId", "item_row"."gameId") THEN
                RAISE EXCEPTION 'Team % must defeat every miniboss before unlocking boss location %', NEW."teamId", NEW."targetId";
            END IF;

        ELSE
            NULL;
    END CASE;

    SELECT count(*) + coalesce((
        SELECT "teams"."additionalScoreAwarded"
        FROM "public"."Teams" AS "teams"
        WHERE "teams"."id" = NEW."teamId"
    ), 0)
    INTO "team_score"
    FROM "public"."TasksLedger" AS "ledger"
    WHERE "ledger"."teamId" = NEW."teamId"
      AND "ledger"."isSuccess" IS TRUE;

    SELECT coalesce(sum(coalesce("items"."price", 0)), 0)
    INTO "spent_points"
    FROM "public"."Shop" AS "shop"
    JOIN "public"."Items" AS "items"
      ON "items"."id" = "shop"."itemId"
    WHERE "shop"."teamId" = NEW."teamId";

    IF ("team_score" - "spent_points") < coalesce("item_row"."price", 0) THEN
        RAISE EXCEPTION 'Team % does not have enough score for item %', NEW."teamId", NEW."itemId";
    END IF;

    RETURN NEW;
END;
$$;

DROP POLICY IF EXISTS "Allow public team reads" ON "public"."Teams";
CREATE POLICY "Allow public team reads"
    ON "public"."Teams"
    FOR SELECT
    TO "anon", "authenticated"
    USING (true);

DROP POLICY IF EXISTS "Allow public team updates" ON "public"."Teams";
CREATE POLICY "Allow public team updates"
    ON "public"."Teams"
    FOR UPDATE
    TO "anon", "authenticated"
    USING (true)
    WITH CHECK ("teamAssignmentId" IS NOT NULL);
