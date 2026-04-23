CREATE POLICY "Allow public game reads"
    ON "public"."Games"
    FOR SELECT
    TO "anon", "authenticated"
    USING (true);

CREATE POLICY "Allow public game creation"
    ON "public"."Games"
    FOR INSERT
    TO "anon", "authenticated"
    WITH CHECK (true);

CREATE POLICY "Allow public team assignment reads"
    ON "public"."TeamAssignment"
    FOR SELECT
    TO "anon", "authenticated"
    USING (true);

CREATE POLICY "Allow public team assignment creation"
    ON "public"."TeamAssignment"
    FOR INSERT
    TO "anon", "authenticated"
    WITH CHECK ("gameId" IS NOT NULL);
