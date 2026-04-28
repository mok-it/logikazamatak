ALTER TABLE IF EXISTS "public"."ItemEffect"
    RENAME TO "ItemEffects";

ALTER SEQUENCE IF EXISTS "public"."ItemEffect_id_seq"
    RENAME TO "ItemEffects_id_seq";

ALTER TABLE IF EXISTS "public"."Item"
    RENAME TO "Items";

ALTER SEQUENCE IF EXISTS "public"."Item_id_seq"
    RENAME TO "Items_id_seq";

ALTER TABLE "public"."Items"
    ADD COLUMN IF NOT EXISTS "gameId" bigint,
    ADD COLUMN IF NOT EXISTS "maxPerTeam" integer;

ALTER TABLE ONLY "public"."Items"
    DROP CONSTRAINT IF EXISTS "Item_itemEffectId_fkey";

ALTER TABLE ONLY "public"."Items"
    ADD CONSTRAINT "Items_itemEffectId_fkey" FOREIGN KEY ("itemEffectId") REFERENCES "public"."ItemEffects"("id");

ALTER TABLE ONLY "public"."Items"
    ADD CONSTRAINT "Items_gameId_fkey" FOREIGN KEY ("gameId") REFERENCES "public"."Games"("id");

ALTER TABLE ONLY "public"."Shop"
    DROP CONSTRAINT IF EXISTS "Shop_itemId_fkey";

ALTER TABLE ONLY "public"."Shop"
    ADD CONSTRAINT "Shop_itemId_fkey" FOREIGN KEY ("itemId") REFERENCES "public"."Items"("id");

ALTER TABLE "public"."ItemEffects" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."Items" ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow public location reads"
    ON "public"."Locations"
    FOR SELECT
    TO "anon", "authenticated"
    USING (true);

CREATE POLICY "Allow public location creation"
    ON "public"."Locations"
    FOR INSERT
    TO "anon", "authenticated"
    WITH CHECK ("gameId" IS NOT NULL);

CREATE POLICY "Allow public task creation"
    ON "public"."Tasks"
    FOR INSERT
    TO "anon", "authenticated"
    WITH CHECK ("gameId" IS NOT NULL);

CREATE POLICY "Allow public item effect reads"
    ON "public"."ItemEffects"
    FOR SELECT
    TO "anon", "authenticated"
    USING (true);

CREATE POLICY "Allow public item reads"
    ON "public"."Items"
    FOR SELECT
    TO "anon", "authenticated"
    USING (true);

CREATE POLICY "Allow public item creation"
    ON "public"."Items"
    FOR INSERT
    TO "anon", "authenticated"
    WITH CHECK ("gameId" IS NOT NULL);
