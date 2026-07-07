ALTER TABLE "public"."ItemEffects"
    ADD COLUMN IF NOT EXISTS "code" text;

UPDATE "public"."ItemEffects"
SET "code" = CASE
    WHEN lower(coalesce("description", '')) IN ('double points for one location', 'területduplázó visszamenőleg') THEN 'retroactive_location_score_multiplier'
    WHEN lower(coalesce("description", '')) IN ('double points for one team area', 'feladatduplázó') THEN 'task_score_multiplier'
    WHEN lower(coalesce("description", '')) = 'feladatduplázó visszamenőleg' THEN 'retroactive_task_score_multiplier'
    ELSE "code"
END
WHERE "code" IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS "ItemEffects_code_key"
    ON "public"."ItemEffects" ("code");

INSERT INTO "public"."ItemEffects" ("code", "description")
VALUES
    ('task_score_multiplier', 'feladatduplázó'),
    ('retroactive_task_score_multiplier', 'feladatduplázó visszamenőleg'),
    ('retroactive_location_score_multiplier', 'területduplázó visszamenőleg'),
    ('miniboss_unlock', 'minibosslelőhely'),
    ('miniboss_rewind', 'idővisszatekerő'),
    ('boss_location_unlock', 'főbosslelőhely')
ON CONFLICT ("code") DO UPDATE
SET "description" = EXCLUDED."description";

ALTER TABLE "public"."ItemEffects"
    ALTER COLUMN "code" SET NOT NULL;

ALTER TABLE "public"."Shop"
    ADD COLUMN IF NOT EXISTS "teamId" bigint;

UPDATE "public"."Shop" AS "shop"
SET "teamId" = "resolved"."teamId"
FROM (
    SELECT
        "shop_inner"."id" AS "shopId",
        "ledger"."teamId"
    FROM "public"."Shop" AS "shop_inner"
    LEFT JOIN LATERAL (
        SELECT "ledger"."teamId"
        FROM "public"."TasksLedger" AS "ledger"
        WHERE "ledger"."userId" = "shop_inner"."userId"
          AND "ledger"."teamId" IS NOT NULL
        ORDER BY "ledger"."created_at" DESC, "ledger"."id" DESC
        LIMIT 1
    ) AS "ledger" ON TRUE
) AS "resolved"
WHERE "shop"."id" = "resolved"."shopId"
  AND "shop"."teamId" IS NULL
  AND "resolved"."teamId" IS NOT NULL;

ALTER TABLE ONLY "public"."Shop"
    DROP CONSTRAINT IF EXISTS "Shop_teamId_fkey";

ALTER TABLE ONLY "public"."Shop"
    ADD CONSTRAINT "Shop_teamId_fkey" FOREIGN KEY ("teamId") REFERENCES "public"."Teams"("id");

CREATE INDEX IF NOT EXISTS "Shop_itemId_teamId_idx"
    ON "public"."Shop" ("itemId", "teamId");

CREATE INDEX IF NOT EXISTS "Shop_teamId_targetId_idx"
    ON "public"."Shop" ("teamId", "targetId");

ALTER TABLE "public"."TasksLedger"
    ADD COLUMN IF NOT EXISTS "bonusSourceShopId" bigint,
    ADD COLUMN IF NOT EXISTS "bonusSourceTasksLedgerId" bigint;

ALTER TABLE ONLY "public"."TasksLedger"
    DROP CONSTRAINT IF EXISTS "TasksLedger_bonusSourceShopId_fkey";

ALTER TABLE ONLY "public"."TasksLedger"
    ADD CONSTRAINT "TasksLedger_bonusSourceShopId_fkey"
        FOREIGN KEY ("bonusSourceShopId") REFERENCES "public"."Shop"("id");

ALTER TABLE ONLY "public"."TasksLedger"
    DROP CONSTRAINT IF EXISTS "TasksLedger_bonusSourceTasksLedgerId_fkey";

ALTER TABLE ONLY "public"."TasksLedger"
    ADD CONSTRAINT "TasksLedger_bonusSourceTasksLedgerId_fkey"
        FOREIGN KEY ("bonusSourceTasksLedgerId") REFERENCES "public"."TasksLedger"("id");

ALTER TABLE ONLY "public"."TasksLedger"
    DROP CONSTRAINT IF EXISTS "TasksLedger_bonus_sources_check";

ALTER TABLE ONLY "public"."TasksLedger"
    ADD CONSTRAINT "TasksLedger_bonus_sources_check"
        CHECK (
            ("bonusSourceShopId" IS NULL AND "bonusSourceTasksLedgerId" IS NULL)
            OR (
                "bonusSourceShopId" IS NOT NULL
                AND "bonusSourceTasksLedgerId" IS NOT NULL
                AND "isSuccess" IS TRUE
            )
        );

CREATE UNIQUE INDEX IF NOT EXISTS "TasksLedger_bonus_source_pair_key"
    ON "public"."TasksLedger" ("bonusSourceShopId", "bonusSourceTasksLedgerId")
    WHERE "bonusSourceShopId" IS NOT NULL
      AND "bonusSourceTasksLedgerId" IS NOT NULL;

CREATE INDEX IF NOT EXISTS "TasksLedger_teamId_taskId_success_idx"
    ON "public"."TasksLedger" ("teamId", "taskId", "isSuccess");

CREATE OR REPLACE FUNCTION "public"."is_base_task_attempt"(
    "bonus_source_shop_id" bigint
) RETURNS boolean
LANGUAGE sql
IMMUTABLE
AS $$
    SELECT "bonus_source_shop_id" IS NULL;
$$;

CREATE OR REPLACE FUNCTION "public"."is_miniboss_task"(
    "task_id" bigint
) RETURNS boolean
LANGUAGE sql
STABLE
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM "public"."Tasks"
        WHERE "id" = "task_id"
          AND "isMiniBoss" IS TRUE
    );
$$;

CREATE OR REPLACE FUNCTION "public"."task_location_id"(
    "task_id" bigint
) RETURNS bigint
LANGUAGE sql
STABLE
AS $$
    SELECT "locationId"
    FROM "public"."Tasks"
    WHERE "id" = "task_id";
$$;

CREATE OR REPLACE FUNCTION "public"."team_has_effect_purchase"(
    "team_id" bigint,
    "target_id" bigint,
    "effect_code" text
) RETURNS boolean
LANGUAGE sql
STABLE
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM "public"."Shop" AS "shop"
        JOIN "public"."Items" AS "items"
          ON "items"."id" = "shop"."itemId"
        JOIN "public"."ItemEffects" AS "effects"
          ON "effects"."id" = "items"."itemEffectId"
        WHERE "shop"."teamId" = "team_id"
          AND "shop"."targetId" = "target_id"
          AND "effects"."code" = "effect_code"
    );
$$;

CREATE OR REPLACE FUNCTION "public"."team_has_task_specific_doubler"(
    "team_id" bigint,
    "task_id" bigint
) RETURNS boolean
LANGUAGE sql
STABLE
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM "public"."Shop" AS "shop"
        JOIN "public"."Items" AS "items"
          ON "items"."id" = "shop"."itemId"
        JOIN "public"."ItemEffects" AS "effects"
          ON "effects"."id" = "items"."itemEffectId"
        WHERE "shop"."teamId" = "team_id"
          AND "shop"."targetId" = "task_id"
          AND "effects"."code" IN (
              'task_score_multiplier',
              'retroactive_task_score_multiplier'
          )
    );
$$;

CREATE OR REPLACE FUNCTION "public"."team_has_location_doubler"(
    "team_id" bigint,
    "location_id" bigint
) RETURNS boolean
LANGUAGE sql
STABLE
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM "public"."Shop" AS "shop"
        JOIN "public"."Items" AS "items"
          ON "items"."id" = "shop"."itemId"
        JOIN "public"."ItemEffects" AS "effects"
          ON "effects"."id" = "items"."itemEffectId"
        WHERE "shop"."teamId" = "team_id"
          AND "shop"."targetId" = "location_id"
          AND "effects"."code" = 'retroactive_location_score_multiplier'
    );
$$;

CREATE OR REPLACE FUNCTION "public"."team_has_miniboss_unlock"(
    "team_id" bigint,
    "task_id" bigint
) RETURNS boolean
LANGUAGE sql
STABLE
AS $$
    SELECT "public"."team_has_effect_purchase"("team_id", "task_id", 'miniboss_unlock');
$$;

CREATE OR REPLACE FUNCTION "public"."team_has_miniboss_rewind"(
    "team_id" bigint,
    "task_id" bigint
) RETURNS boolean
LANGUAGE sql
STABLE
AS $$
    SELECT "public"."team_has_effect_purchase"("team_id", "task_id", 'miniboss_rewind');
$$;

CREATE OR REPLACE FUNCTION "public"."team_has_miniboss_failed_base_attempt"(
    "team_id" bigint,
    "task_id" bigint
) RETURNS boolean
LANGUAGE sql
STABLE
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM "public"."TasksLedger"
        WHERE "teamId" = "team_id"
          AND "taskId" = "task_id"
          AND "isSuccess" IS FALSE
          AND "public"."is_base_task_attempt"("bonusSourceShopId")
    );
$$;

CREATE OR REPLACE FUNCTION "public"."team_has_miniboss_success"(
    "team_id" bigint,
    "task_id" bigint
) RETURNS boolean
LANGUAGE sql
STABLE
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM "public"."TasksLedger"
        WHERE "teamId" = "team_id"
          AND "taskId" = "task_id"
          AND "isSuccess" IS TRUE
          AND "public"."is_base_task_attempt"("bonusSourceShopId")
    );
$$;

CREATE OR REPLACE FUNCTION "public"."team_defeated_all_minibosses"(
    "team_id" bigint,
    "game_id" bigint
) RETURNS boolean
LANGUAGE sql
STABLE
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM "public"."Tasks" AS "tasks"
        WHERE "tasks"."gameId" = "game_id"
          AND "tasks"."isMiniBoss" IS TRUE
    )
    AND NOT EXISTS (
        SELECT 1
        FROM "public"."Tasks" AS "tasks"
        WHERE "tasks"."gameId" = "game_id"
          AND "tasks"."isMiniBoss" IS TRUE
          AND NOT "public"."team_has_miniboss_success"("team_id", "tasks"."id")
    );
$$;

CREATE OR REPLACE FUNCTION "public"."validate_shop_purchase"()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    "item_row" record;
    "task_location_id" bigint;
BEGIN
    IF NEW."itemId" IS NULL THEN
        RAISE EXCEPTION 'Shop purchase requires itemId';
    END IF;

    IF NEW."teamId" IS NULL THEN
        RAISE EXCEPTION 'Shop purchase requires teamId';
    END IF;

    SELECT
        "items"."id",
        "items"."gameId",
        "items"."maxPerTeam",
        "items"."price",
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

    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION "public"."validate_tasks_ledger_insert"()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NOT "public"."is_base_task_attempt"(NEW."bonusSourceShopId")
       OR NEW."teamId" IS NULL
       OR NOT "public"."is_miniboss_task"(NEW."taskId") THEN
        RETURN NEW;
    END IF;

    IF NOT "public"."team_has_miniboss_unlock"(NEW."teamId", NEW."taskId") THEN
        RAISE EXCEPTION 'Team % must unlock miniboss task % before attempting it', NEW."teamId", NEW."taskId";
    END IF;

    IF "public"."team_has_miniboss_success"(NEW."teamId", NEW."taskId") THEN
        RAISE EXCEPTION 'Miniboss task % is already defeated by team %', NEW."taskId", NEW."teamId";
    END IF;

    IF "public"."team_has_miniboss_failed_base_attempt"(NEW."teamId", NEW."taskId")
       AND NOT "public"."team_has_miniboss_rewind"(NEW."teamId", NEW."taskId") THEN
        RAISE EXCEPTION 'Team % must buy a miniboss rewind before retrying task %', NEW."teamId", NEW."taskId";
    END IF;

    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION "public"."apply_shop_purchase_effect"()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    "effect_code" text;
BEGIN
    SELECT "effects"."code"
    INTO "effect_code"
    FROM "public"."Items" AS "items"
    JOIN "public"."ItemEffects" AS "effects"
      ON "effects"."id" = "items"."itemEffectId"
    WHERE "items"."id" = NEW."itemId";

    CASE "effect_code"
        WHEN 'retroactive_task_score_multiplier' THEN
            INSERT INTO "public"."TasksLedger" (
                "taskId",
                "teamId",
                "userId",
                "isSuccess",
                "bonusSourceShopId",
                "bonusSourceTasksLedgerId"
            )
            SELECT
                "ledger"."taskId",
                "ledger"."teamId",
                coalesce(NEW."userId", "ledger"."userId"),
                TRUE,
                NEW."id",
                "ledger"."id"
            FROM "public"."TasksLedger" AS "ledger"
            WHERE "ledger"."teamId" = NEW."teamId"
              AND "ledger"."taskId" = NEW."targetId"
              AND "ledger"."isSuccess" IS TRUE
              AND "public"."is_base_task_attempt"("ledger"."bonusSourceShopId")
              AND NOT EXISTS (
                  SELECT 1
                  FROM "public"."TasksLedger" AS "bonus"
                  WHERE "bonus"."bonusSourceTasksLedgerId" = "ledger"."id"
              );

        WHEN 'retroactive_location_score_multiplier' THEN
            INSERT INTO "public"."TasksLedger" (
                "taskId",
                "teamId",
                "userId",
                "isSuccess",
                "bonusSourceShopId",
                "bonusSourceTasksLedgerId"
            )
            SELECT
                "ledger"."taskId",
                "ledger"."teamId",
                coalesce(NEW."userId", "ledger"."userId"),
                TRUE,
                NEW."id",
                "ledger"."id"
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
              );
        ELSE
            NULL;
    END CASE;

    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION "public"."apply_task_score_multiplier_on_success"()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW."isSuccess" IS NOT TRUE
       OR NEW."teamId" IS NULL
       OR NOT "public"."is_base_task_attempt"(NEW."bonusSourceShopId") THEN
        RETURN NEW;
    END IF;

    INSERT INTO "public"."TasksLedger" (
        "taskId",
        "teamId",
        "userId",
        "isSuccess",
        "bonusSourceShopId",
        "bonusSourceTasksLedgerId"
    )
    SELECT
        NEW."taskId",
        NEW."teamId",
        coalesce(NEW."userId", "shop"."userId"),
        TRUE,
        "shop"."id",
        NEW."id"
    FROM "public"."Shop" AS "shop"
    JOIN "public"."Items" AS "items"
      ON "items"."id" = "shop"."itemId"
    JOIN "public"."ItemEffects" AS "effects"
      ON "effects"."id" = "items"."itemEffectId"
    WHERE "shop"."teamId" = NEW."teamId"
      AND "shop"."targetId" = NEW."taskId"
      AND "effects"."code" = 'task_score_multiplier'
      AND NOT EXISTS (
          SELECT 1
          FROM "public"."TasksLedger" AS "bonus"
          WHERE "bonus"."bonusSourceShopId" = "shop"."id"
            AND "bonus"."bonusSourceTasksLedgerId" = NEW."id"
      );

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS "validate_shop_purchase" ON "public"."Shop";
CREATE TRIGGER "validate_shop_purchase"
    BEFORE INSERT ON "public"."Shop"
    FOR EACH ROW
    EXECUTE FUNCTION "public"."validate_shop_purchase"();

DROP TRIGGER IF EXISTS "validate_tasks_ledger_insert" ON "public"."TasksLedger";
CREATE TRIGGER "validate_tasks_ledger_insert"
    BEFORE INSERT ON "public"."TasksLedger"
    FOR EACH ROW
    EXECUTE FUNCTION "public"."validate_tasks_ledger_insert"();

DROP TRIGGER IF EXISTS "apply_shop_purchase_effect" ON "public"."Shop";
CREATE TRIGGER "apply_shop_purchase_effect"
    AFTER INSERT ON "public"."Shop"
    FOR EACH ROW
    EXECUTE FUNCTION "public"."apply_shop_purchase_effect"();

DROP TRIGGER IF EXISTS "apply_task_score_multiplier_on_success" ON "public"."TasksLedger";
CREATE TRIGGER "apply_task_score_multiplier_on_success"
    AFTER INSERT ON "public"."TasksLedger"
    FOR EACH ROW
    EXECUTE FUNCTION "public"."apply_task_score_multiplier_on_success"();

DROP POLICY IF EXISTS "Allow public shop reads" ON "public"."Shop";
CREATE POLICY "Allow public shop reads"
    ON "public"."Shop"
    FOR SELECT
    TO "anon", "authenticated"
    USING (true);

DROP POLICY IF EXISTS "Allow public shop creation" ON "public"."Shop";
CREATE POLICY "Allow public shop creation"
    ON "public"."Shop"
    FOR INSERT
    TO "anon", "authenticated"
    WITH CHECK (
        "itemId" IS NOT NULL
        AND "teamId" IS NOT NULL
    );
