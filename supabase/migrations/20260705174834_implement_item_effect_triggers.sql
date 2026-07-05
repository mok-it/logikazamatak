ALTER TABLE "public"."ItemEffects"
    ADD COLUMN IF NOT EXISTS "code" text;

UPDATE "public"."ItemEffects"
SET
    "code" = CASE
        WHEN "code" IS NOT NULL THEN "code"
        WHEN lower(coalesce("description", '')) LIKE '%retroactive%location%' THEN 'retroactive_location_score_multiplier'
        WHEN lower(coalesce("description", '')) LIKE '%retroactive%task%' THEN 'retroactive_task_score_multiplier'
        WHEN lower(coalesce("description", '')) LIKE '%task%' THEN 'task_score_multiplier'
        WHEN lower(coalesce("description", '')) LIKE '%location%' THEN 'retroactive_location_score_multiplier'
        WHEN lower(coalesce("description", '')) LIKE '%area%' THEN 'task_score_multiplier'
        ELSE concat('legacy_item_effect_', "id")
    END,
    "description" = CASE
        WHEN lower(coalesce("description", '')) LIKE '%retroactive%location%' THEN 'Retroactive location score multiplier'
        WHEN lower(coalesce("description", '')) LIKE '%retroactive%task%' THEN 'Retroactive task score multiplier'
        WHEN lower(coalesce("description", '')) LIKE '%task%' THEN 'Task score multiplier'
        WHEN lower(coalesce("description", '')) LIKE '%location%' THEN 'Retroactive location score multiplier'
        WHEN lower(coalesce("description", '')) LIKE '%area%' THEN 'Task score multiplier'
        ELSE coalesce("description", concat('Legacy item effect ', "id"))
    END
WHERE "code" IS NULL
   OR lower(coalesce("description", '')) IN (
       'double points for one location',
       'double points for one team area'
   );

ALTER TABLE "public"."ItemEffects"
    ALTER COLUMN "code" SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS "ItemEffects_code_key"
    ON "public"."ItemEffects" ("code");

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

CREATE OR REPLACE FUNCTION "public"."validate_shop_purchase"()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    "item_row" record;
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

            IF NOT EXISTS (
                SELECT 1
                FROM "public"."Tasks" AS "tasks"
                WHERE "tasks"."id" = NEW."targetId"
                  AND "tasks"."gameId" = "item_row"."gameId"
            ) THEN
                RAISE EXCEPTION 'Task % is not part of item game %', NEW."targetId", "item_row"."gameId";
            END IF;

            IF EXISTS (
                SELECT 1
                FROM "public"."TasksLedger" AS "ledger"
                WHERE "ledger"."teamId" = NEW."teamId"
                  AND "ledger"."taskId" = NEW."targetId"
                  AND "ledger"."isSuccess" IS TRUE
                  AND "ledger"."bonusSourceShopId" IS NULL
            ) THEN
                RAISE EXCEPTION 'Task % already has earned score; use the retroactive task multiplier instead', NEW."targetId";
            END IF;

            IF EXISTS (
                SELECT 1
                FROM "public"."Shop" AS "shop"
                JOIN "public"."Items" AS "items"
                  ON "items"."id" = "shop"."itemId"
                JOIN "public"."ItemEffects" AS "effects"
                  ON "effects"."id" = "items"."itemEffectId"
                WHERE "shop"."teamId" = NEW."teamId"
                  AND "shop"."targetId" = NEW."targetId"
                  AND "effects"."code" IN (
                      'task_score_multiplier',
                      'retroactive_task_score_multiplier'
                  )
            ) THEN
                RAISE EXCEPTION 'Task % already has a score multiplier for team %', NEW."targetId", NEW."teamId";
            END IF;

        WHEN 'retroactive_task_score_multiplier' THEN
            IF NEW."targetId" IS NULL THEN
                RAISE EXCEPTION 'Retroactive task score multiplier purchases require a task target';
            END IF;

            IF NOT EXISTS (
                SELECT 1
                FROM "public"."Tasks" AS "tasks"
                WHERE "tasks"."id" = NEW."targetId"
                  AND "tasks"."gameId" = "item_row"."gameId"
            ) THEN
                RAISE EXCEPTION 'Task % is not part of item game %', NEW."targetId", "item_row"."gameId";
            END IF;

            IF EXISTS (
                SELECT 1
                FROM "public"."Shop" AS "shop"
                JOIN "public"."Items" AS "items"
                  ON "items"."id" = "shop"."itemId"
                JOIN "public"."ItemEffects" AS "effects"
                  ON "effects"."id" = "items"."itemEffectId"
                WHERE "shop"."teamId" = NEW."teamId"
                  AND "shop"."targetId" = NEW."targetId"
                  AND "effects"."code" IN (
                      'task_score_multiplier',
                      'retroactive_task_score_multiplier'
                  )
            ) THEN
                RAISE EXCEPTION 'Task % already has a score multiplier for team %', NEW."targetId", NEW."teamId";
            END IF;

            IF NOT EXISTS (
                SELECT 1
                FROM "public"."TasksLedger" AS "ledger"
                WHERE "ledger"."teamId" = NEW."teamId"
                  AND "ledger"."taskId" = NEW."targetId"
                  AND "ledger"."isSuccess" IS TRUE
                  AND "ledger"."bonusSourceShopId" IS NULL
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

            IF EXISTS (
                SELECT 1
                FROM "public"."Shop" AS "shop"
                JOIN "public"."Items" AS "items"
                  ON "items"."id" = "shop"."itemId"
                JOIN "public"."ItemEffects" AS "effects"
                  ON "effects"."id" = "items"."itemEffectId"
                WHERE "shop"."teamId" = NEW."teamId"
                  AND "shop"."targetId" = NEW."targetId"
                  AND "effects"."code" = 'retroactive_location_score_multiplier'
            ) THEN
                RAISE EXCEPTION 'Location % already has a retroactive multiplier for team %', NEW."targetId", NEW."teamId";
            END IF;

            IF NOT EXISTS (
                SELECT 1
                FROM "public"."TasksLedger" AS "ledger"
                JOIN "public"."Tasks" AS "tasks"
                  ON "tasks"."id" = "ledger"."taskId"
                WHERE "ledger"."teamId" = NEW."teamId"
                  AND "ledger"."isSuccess" IS TRUE
                  AND "ledger"."bonusSourceShopId" IS NULL
                  AND "tasks"."locationId" = NEW."targetId"
                  AND NOT EXISTS (
                      SELECT 1
                      FROM "public"."TasksLedger" AS "bonus"
                      WHERE "bonus"."bonusSourceTasksLedgerId" = "ledger"."id"
                  )
            ) THEN
                RAISE EXCEPTION 'Location % has no completed score left to double for team %', NEW."targetId", NEW."teamId";
            END IF;
        ELSE
            NULL;
    END CASE;

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
              AND "ledger"."bonusSourceShopId" IS NULL
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
              AND "ledger"."bonusSourceShopId" IS NULL
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
       OR NEW."bonusSourceShopId" IS NOT NULL THEN
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
