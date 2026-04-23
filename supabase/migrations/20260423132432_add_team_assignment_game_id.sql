ALTER TABLE "public"."TeamAssignment"
    ADD COLUMN "gameId" bigint;

ALTER TABLE ONLY "public"."TeamAssignment"
    ADD CONSTRAINT "TeamAssignment_gameId_fkey" FOREIGN KEY ("gameId") REFERENCES "public"."Games"("id");
