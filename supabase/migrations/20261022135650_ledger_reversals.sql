ALTER TABLE public."TasksLedger" ADD COLUMN "reversesId" bigint UNIQUE REFERENCES public."TasksLedger"(id);
ALTER TABLE public."HealingLedger" ADD COLUMN "reversesId" bigint UNIQUE REFERENCES public."HealingLedger"(id);
ALTER TABLE public."Shop" ADD COLUMN "reversesId" bigint UNIQUE REFERENCES public."Shop"(id);
-- Reversals are new rows. Original rows and timestamps remain untouched.
CREATE VIEW public."ActiveTasksLedger" WITH (security_invoker = true) AS
SELECT entry.* FROM public."TasksLedger" entry
WHERE entry."reversesId" IS NULL
AND NOT EXISTS (SELECT 1 FROM public."TasksLedger" reversal WHERE reversal."reversesId" = entry.id)
AND NOT EXISTS (SELECT 1 FROM public."Shop" reversal WHERE reversal."reversesId" = entry."bonusSourceShopId")
AND NOT EXISTS (SELECT 1 FROM public."TasksLedger" reversal WHERE reversal."reversesId" = entry."bonusSourceTasksLedgerId");
GRANT SELECT ON public."ActiveTasksLedger" TO anon, authenticated;

CREATE VIEW public."ActiveHealingLedger" WITH (security_invoker = true) AS
SELECT entry.* FROM public."HealingLedger" entry
WHERE entry."reversesId" IS NULL
AND NOT EXISTS (SELECT 1 FROM public."HealingLedger" reversal WHERE reversal."reversesId" = entry.id);
GRANT SELECT ON public."ActiveHealingLedger" TO anon, authenticated;

CREATE VIEW public."ActiveShop" WITH (security_invoker = true) AS
SELECT entry.* FROM public."Shop" entry
WHERE entry."reversesId" IS NULL
AND NOT EXISTS (SELECT 1 FROM public."Shop" reversal WHERE reversal."reversesId" = entry.id);
GRANT SELECT ON public."ActiveShop" TO anon, authenticated;

DROP INDEX public."TasksLedger_bonus_source_pair_key";
CREATE UNIQUE INDEX "TasksLedger_bonus_source_pair_key"
ON public."TasksLedger" ("bonusSourceShopId", "bonusSourceTasksLedgerId")
WHERE "bonusSourceShopId" IS NOT NULL AND "bonusSourceTasksLedgerId" IS NOT NULL AND "reversesId" IS NULL;

-- Keep existing item rules; change their reads to the same active entries used by clients.
DO $$
DECLARE fn record; definition text;
BEGIN
    FOR fn IN SELECT p.oid, p.proname FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
        WHERE n.nspname = 'public' AND p.proname IN (
            'team_has_effect_purchase', 'team_has_task_specific_doubler', 'team_has_location_doubler',
            'team_has_miniboss_failed_base_attempt', 'team_has_miniboss_success',
            'team_has_effect_purchase_any_target', 'validate_shop_purchase', 'validate_tasks_ledger_insert',
            'apply_shop_purchase_effect', 'apply_task_score_multiplier_on_success'
        )
    LOOP
        definition := pg_get_functiondef(fn.oid);
        definition := replace(definition, 'FROM "public"."TasksLedger"', 'FROM "public"."ActiveTasksLedger"');
        definition := replace(definition, 'JOIN "public"."TasksLedger"', 'JOIN "public"."ActiveTasksLedger"');
        definition := replace(definition, 'FROM "public"."Shop"', 'FROM "public"."ActiveShop"');
        definition := replace(definition, 'JOIN "public"."Shop"', 'JOIN "public"."ActiveShop"');
        IF fn.proname IN ('validate_shop_purchase', 'validate_tasks_ledger_insert',
            'apply_shop_purchase_effect', 'apply_task_score_multiplier_on_success') THEN
            definition := replace(definition, E'BEGIN\n', E'BEGIN\n    IF NEW."reversesId" IS NOT NULL THEN RETURN NEW; END IF;\n');
        END IF;
        EXECUTE definition;
    END LOOP;
END;
$$;

CREATE FUNCTION public.guard_ledger_entry() RETURNS trigger LANGUAGE plpgsql
SET search_path = '' AS $$
DECLARE original jsonb; balance bigint; delta bigint := 0;
BEGIN
    IF TG_OP <> 'INSERT' THEN
        RAISE EXCEPTION 'A napló nem módosítható és nem törölhető. Használd a visszavonást.';
    END IF;
    IF NEW."teamId" IS NULL THEN RAISE EXCEPTION 'A tranzakcióhoz csapat szükséges.'; END IF;
    PERFORM pg_catalog.pg_advisory_xact_lock(NEW."teamId");
    IF TG_TABLE_NAME = 'HealingLedger' AND NEW."reversesId" IS NULL THEN
        IF NOT EXISTS (SELECT 1 FROM public."ActiveTasksLedger" e WHERE e.id = NEW."healedTasksLedgerId"
            AND e."teamId" = NEW."teamId" AND e."isSuccess" IS FALSE) THEN
            RAISE EXCEPTION 'Csak aktív, elbukott csapatpróba gyógyítható.';
        END IF;
        IF EXISTS (SELECT 1 FROM public."ActiveHealingLedger" e WHERE e."healedTasksLedgerId" = NEW."healedTasksLedgerId") THEN
            RAISE EXCEPTION 'Ez a próba már meg lett gyógyítva.';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM public."HealingTasks" h JOIN public."TeamAssignment" a ON a."gameId" = h."gameId"
            JOIN public."Teams" t ON t."teamAssignmentId" = a.id WHERE h.id = NEW."healingTaskId" AND t.id = NEW."teamId") THEN
            RAISE EXCEPTION 'A gyógyító feladat nem ehhez a játékhoz tartozik.';
        END IF;
    END IF;
    IF NEW."reversesId" IS NOT NULL THEN
        EXECUTE format('SELECT to_jsonb(e) FROM public.%I e WHERE id = $1', TG_TABLE_NAME)
            INTO original USING NEW."reversesId";
        IF original IS NULL OR original->>'reversesId' IS NOT NULL THEN
            RAISE EXCEPTION 'Csak eredeti tranzakció vonható vissza.';
        END IF;
        IF pg_catalog.pg_trigger_depth() = 1 THEN
            SELECT count(*) + coalesce((SELECT "additionalScoreAwarded" FROM public."Teams" WHERE id = NEW."teamId"), 0)
                - coalesce((SELECT sum(i.price) FROM public."ActiveShop" s JOIN public."Items" i ON i.id = s."itemId"
                            WHERE s."teamId" = NEW."teamId"), 0)
            INTO balance FROM public."ActiveTasksLedger" WHERE "teamId" = NEW."teamId" AND "isSuccess" IS TRUE;
            IF TG_TABLE_NAME = 'TasksLedger' THEN
                SELECT -(CASE WHEN (original->>'isSuccess')::boolean IS TRUE THEN 1 ELSE 0 END) - count(*)
                INTO delta FROM public."ActiveTasksLedger" WHERE "bonusSourceTasksLedgerId" = NEW."reversesId";
            ELSIF TG_TABLE_NAME = 'Shop' THEN
                SELECT coalesce((SELECT price FROM public."Items" WHERE id = (original->>'itemId')::bigint), 0) - count(*)
                INTO delta FROM public."ActiveTasksLedger" WHERE "bonusSourceShopId" = NEW."reversesId";
            END IF;
            IF balance + delta < least(balance, 0) THEN
                RAISE EXCEPTION 'Előbb vond vissza a felhasznált pontokból történt vásárlásokat.';
            END IF;
        END IF;
        IF TG_TABLE_NAME = 'TasksLedger' AND original->>'bonusSourceShopId' IS NOT NULL
            AND pg_catalog.pg_trigger_depth() = 1 THEN
            RAISE EXCEPTION 'A bónusz az eredeti próbával vagy vásárlással vonható vissza.';
        END IF;
        IF (to_jsonb(NEW) - ARRAY['id','created_at','userId','reversesId'])
            IS DISTINCT FROM (original - ARRAY['id','created_at','userId','reversesId']) THEN
            RAISE EXCEPTION 'A visszavonásnak az eredeti tranzakciót kell tükröznie.';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

CREATE FUNCTION public.apply_ledger_reversal() RETURNS trigger LANGUAGE plpgsql
SET search_path = '' AS $$
DECLARE game_id bigint;
BEGIN
    IF NEW."reversesId" IS NULL THEN RETURN NEW; END IF;
    IF TG_TABLE_NAME IN ('TasksLedger', 'Shop') THEN
        INSERT INTO public."TasksLedger" ("taskId", "teamId", "isSuccess", "bonusSourceShopId", "bonusSourceTasksLedgerId", "reversesId")
        SELECT e."taskId", e."teamId", e."isSuccess", e."bonusSourceShopId", e."bonusSourceTasksLedgerId", e.id
        FROM public."TasksLedger" e WHERE e."reversesId" IS NULL
        AND ((TG_TABLE_NAME = 'TasksLedger' AND e."bonusSourceTasksLedgerId" = NEW."reversesId")
            OR (TG_TABLE_NAME = 'Shop' AND e."bonusSourceShopId" = NEW."reversesId"))
        AND NOT EXISTS (SELECT 1 FROM public."TasksLedger" r WHERE r."reversesId" = e.id);
    END IF;
    IF TG_TABLE_NAME = 'TasksLedger' THEN
        INSERT INTO public."HealingLedger" ("teamId", "healingTaskId", "healedTasksLedgerId", "reversesId")
        SELECT e."teamId", e."healingTaskId", e."healedTasksLedgerId", e.id
        FROM public."ActiveHealingLedger" e WHERE e."healedTasksLedgerId" = NEW."reversesId";
    END IF;


    IF EXISTS (
        SELECT 1 FROM public."ActiveTasksLedger" e JOIN public."Tasks" t ON t.id = e."taskId"
        WHERE e."teamId" = NEW."teamId" AND t."isMiniBoss" IS TRUE AND e."bonusSourceShopId" IS NULL
        AND NOT public.team_has_miniboss_unlock(NEW."teamId", t.id)
    ) THEN RAISE EXCEPTION 'Előbb vond vissza a feloldást használó feladatpróbákat.'; END IF;
    IF EXISTS (
        SELECT 1 FROM public."ActiveTasksLedger" e JOIN public."Tasks" t ON t.id = e."taskId"
        WHERE e."teamId" = NEW."teamId" AND t."isMiniBoss" IS TRUE AND e."bonusSourceShopId" IS NULL
        AND NOT public.team_has_miniboss_rewind(NEW."teamId", t.id)
        GROUP BY t.id HAVING count(*) > 1
    ) THEN RAISE EXCEPTION 'Előbb vond vissza az újrapróbálkozást.'; END IF;
    SELECT a."gameId" INTO game_id FROM public."Teams" t JOIN public."TeamAssignment" a ON a.id = t."teamAssignmentId"
        WHERE t.id = NEW."teamId";
    IF public.team_has_effect_purchase_any_target(NEW."teamId", 'boss_location_unlock')
        AND NOT public.team_defeated_all_minibosses(NEW."teamId", game_id) THEN
        RAISE EXCEPTION 'Előbb vond vissza a főellenség feloldását.';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER a_guard_ledger_entry BEFORE INSERT OR UPDATE OR DELETE ON public."TasksLedger"
FOR EACH ROW EXECUTE FUNCTION public.guard_ledger_entry();
CREATE TRIGGER z_apply_ledger_reversal AFTER INSERT ON public."TasksLedger"
FOR EACH ROW EXECUTE FUNCTION public.apply_ledger_reversal();
CREATE TRIGGER a_guard_ledger_entry BEFORE INSERT OR UPDATE OR DELETE ON public."HealingLedger"
FOR EACH ROW EXECUTE FUNCTION public.guard_ledger_entry();
CREATE TRIGGER z_apply_ledger_reversal AFTER INSERT ON public."HealingLedger"
FOR EACH ROW EXECUTE FUNCTION public.apply_ledger_reversal();
CREATE TRIGGER a_guard_ledger_entry BEFORE INSERT OR UPDATE OR DELETE ON public."Shop"
FOR EACH ROW EXECUTE FUNCTION public.guard_ledger_entry();
CREATE TRIGGER z_apply_ledger_reversal AFTER INSERT ON public."Shop"
FOR EACH ROW EXECUTE FUNCTION public.apply_ledger_reversal();

CREATE FUNCTION public.undo_ledger_entry(p_kind text, p_entry_id bigint, p_team_id bigint)
RETURNS bigint LANGUAGE plpgsql SECURITY INVOKER SET search_path = '' AS $$
DECLARE reversal_id bigint;
BEGIN
    CASE p_kind
    WHEN 'task' THEN
        INSERT INTO public."TasksLedger" ("taskId", "teamId", "isSuccess", "bonusSourceShopId", "bonusSourceTasksLedgerId", "reversesId")
        SELECT "taskId", "teamId", "isSuccess", "bonusSourceShopId", "bonusSourceTasksLedgerId", id FROM public."TasksLedger"
        WHERE id = p_entry_id AND "teamId" = p_team_id AND "reversesId" IS NULL
        RETURNING id INTO reversal_id;
    WHEN 'healing' THEN
        INSERT INTO public."HealingLedger" ("teamId", "healingTaskId", "healedTasksLedgerId", "reversesId")
        SELECT "teamId", "healingTaskId", "healedTasksLedgerId", id FROM public."HealingLedger"
        WHERE id = p_entry_id AND "teamId" = p_team_id AND "reversesId" IS NULL
        RETURNING id INTO reversal_id;
    WHEN 'shop' THEN
        INSERT INTO public."Shop" ("itemId", "teamId", "targetId", "reversesId")
        SELECT "itemId", "teamId", "targetId", id FROM public."Shop"
        WHERE id = p_entry_id AND "teamId" = p_team_id AND "reversesId" IS NULL
        RETURNING id INTO reversal_id;
    ELSE RAISE EXCEPTION 'Ismeretlen tranzakciótípus.';
    END CASE;
    IF reversal_id IS NULL THEN RAISE EXCEPTION 'A tranzakció nem található ennél a csapatnál.'; END IF;
    RETURN reversal_id;
END;
$$;
REVOKE ALL ON FUNCTION public.undo_ledger_entry(text, bigint, bigint) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.undo_ledger_entry(text, bigint, bigint) TO anon, authenticated;
