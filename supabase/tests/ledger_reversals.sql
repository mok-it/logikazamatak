-- Run against a migrated local DB with psql -v ON_ERROR_STOP=1 -f supabase/tests/ledger_reversals.sql.
BEGIN;
CREATE TEMP TABLE fixture (game_id bigint, team_id bigint, task_id bigint, healing_task_id bigint, doubler_id bigint);
DO $$
DECLARE g bigint; a bigint; t bigint; l bigint; task bigint; h bigint; item bigint; effect bigint;
BEGIN
    INSERT INTO public."Games" (name) VALUES ('Ledger regression') RETURNING id INTO g;
    INSERT INTO public."TeamAssignment" ("gameId", "baseTeamCounter") VALUES (g, 1) RETURNING id INTO a;
    INSERT INTO public."Teams" (name, "teamAssignmentId", "additionalScoreAwarded") VALUES ('Red', a, 10) RETURNING id INTO t;
    INSERT INTO public."Locations" (name, "gameId") VALUES ('Location', g) RETURNING id INTO l;
    INSERT INTO public."Tasks" (text, solution, "gameId", "locationId", "isMiniBoss") VALUES ('Task', 'answer', g, l, false) RETURNING id INTO task;
    INSERT INTO public."HealingTasks" (text, solution, "gameId") VALUES ('Heal', 'answer', g) RETURNING id INTO h;
    SELECT id INTO effect FROM public."ItemEffects" WHERE code = 'retroactive_task_score_multiplier';
    INSERT INTO public."Items" (name, price, "gameId", "itemEffectId", "maxPerTeam") VALUES ('Double', 1, g, effect, 1) RETURNING id INTO item;
    INSERT INTO fixture VALUES (g, t, task, h, item);
END;
$$;
GRANT SELECT ON fixture TO anon;
SET LOCAL ROLE anon;
DO $$
DECLARE f record; attempt bigint; purchase bigint; failed bigint; heal bigint; rejected boolean; before_count bigint;
BEGIN
    SELECT * INTO f FROM fixture;
    INSERT INTO public."TasksLedger" ("taskId", "teamId", "isSuccess") VALUES (f.task_id, f.team_id, true) RETURNING id INTO attempt;
    INSERT INTO public."Shop" ("itemId", "teamId", "targetId") VALUES (f.doubler_id, f.team_id, f.task_id) RETURNING id INTO purchase;
    ASSERT (SELECT count(*) = 2 FROM public."ActiveTasksLedger" WHERE "teamId" = f.team_id), 'Purchase must create bonus';
    rejected := false;
    BEGIN
        INSERT INTO public."TasksLedger" ("taskId", "teamId", "isSuccess", "reversesId")
            VALUES (f.task_id, f.team_id, false, attempt);
    EXCEPTION WHEN raise_exception THEN rejected := true;
    END;
    ASSERT rejected, 'Forged reversal fields must be rejected';
    rejected := false;
    BEGIN
        PERFORM public.undo_ledger_entry('task', (SELECT id FROM public."ActiveTasksLedger"
            WHERE "bonusSourceShopId" = purchase), f.team_id);
    EXCEPTION WHEN raise_exception THEN rejected := true;
    END;
    ASSERT rejected, 'Derived bonuses must only be undone through their source';
    PERFORM public.undo_ledger_entry('shop', purchase, f.team_id);
    ASSERT NOT EXISTS (SELECT 1 FROM public."ActiveShop" WHERE id = purchase), 'Reversed purchase must not spend money';
    ASSERT (SELECT count(*) = 1 FROM public."ActiveTasksLedger" WHERE "teamId" = f.team_id), 'Purchase undo must reverse bonus';
    ASSERT (SELECT count(*) = 3 FROM public."TasksLedger" WHERE "teamId" = f.team_id), 'Bonus and reversal must stay in history';
    ASSERT (SELECT count(*) = 2 FROM public."Shop" WHERE "teamId" = f.team_id), 'Purchase and reversal must stay in history';
    rejected := false;
    BEGIN PERFORM public.undo_ledger_entry('shop', purchase, f.team_id); EXCEPTION WHEN unique_violation THEN rejected := true; END;
    ASSERT rejected, 'Double undo must be rejected';
    -- Limits and doubler checks must use active purchases, allowing a new purchase after undo.
    INSERT INTO public."Shop" ("itemId", "teamId", "targetId") VALUES (f.doubler_id, f.team_id, f.task_id) RETURNING id INTO purchase;
    PERFORM public.undo_ledger_entry('task', attempt, f.team_id);
    ASSERT NOT EXISTS (SELECT 1 FROM public."ActiveTasksLedger" WHERE "teamId" = f.team_id), 'Task undo must reverse derived bonus';

    INSERT INTO public."TasksLedger" ("taskId", "teamId", "isSuccess") VALUES (f.task_id, f.team_id, false) RETURNING id INTO failed;
    INSERT INTO public."HealingLedger" ("teamId", "healingTaskId", "healedTasksLedgerId") VALUES (f.team_id, f.healing_task_id, failed) RETURNING id INTO heal;
    PERFORM public.undo_ledger_entry('healing', heal, f.team_id);
    ASSERT NOT EXISTS (SELECT 1 FROM public."ActiveHealingLedger" WHERE id = heal), 'Healing undo must restore healability';
    INSERT INTO public."HealingLedger" ("teamId", "healingTaskId", "healedTasksLedgerId") VALUES (f.team_id, f.healing_task_id, failed);
    PERFORM public.undo_ledger_entry('task', failed, f.team_id);
    ASSERT NOT EXISTS (SELECT 1 FROM public."ActiveHealingLedger" WHERE "teamId" = f.team_id), 'Failed-task undo must reverse dependent healing';
    rejected := false;
    BEGIN
        INSERT INTO public."HealingLedger" ("teamId", "healingTaskId", "healedTasksLedgerId") VALUES (f.team_id, f.healing_task_id, failed);
    EXCEPTION WHEN raise_exception THEN rejected := true;
    END;
    ASSERT rejected, 'A stale healing request must not heal a reversed attempt';

    rejected := false;
    BEGIN PERFORM public.undo_ledger_entry('shop', purchase, f.team_id + 100000); EXCEPTION WHEN raise_exception THEN rejected := true; END;
    ASSERT rejected, 'Wrong team must not undo a purchase';
    -- Superuser setup is below; mutation guards must also protect privileged callers.
END;
$$;
RESET ROLE;
DO $$
DECLARE f record; attempt bigint; purchase bigint; effect bigint; item bigint; rejected boolean;
BEGIN
    SELECT * INTO f FROM fixture;
    rejected := false;
    BEGIN DELETE FROM public."TasksLedger" WHERE "teamId" = f.team_id; EXCEPTION WHEN raise_exception THEN rejected := true; END;
    ASSERT rejected, 'History must not be deleted';
    rejected := false;
    BEGIN UPDATE public."Shop" SET "targetId" = NULL WHERE "teamId" = f.team_id; EXCEPTION WHEN raise_exception THEN rejected := true; END;
    ASSERT rejected, 'History must not be edited';
    -- A reversal that would make the existing budget negative must roll back completely.
    INSERT INTO public."Teams" (name, "teamAssignmentId")
        SELECT 'Budget', "teamAssignmentId" FROM public."Teams" WHERE id = f.team_id RETURNING id INTO f.team_id;
    INSERT INTO public."TasksLedger" ("taskId", "teamId", "isSuccess") VALUES (f.task_id, f.team_id, true) RETURNING id INTO attempt;
    INSERT INTO public."ItemEffects" (code, description) VALUES ('ledger_test', 'Regression') RETURNING id INTO effect;
    INSERT INTO public."Items" (name, price, "gameId", "itemEffectId") VALUES ('Spend', 1, f.game_id, effect) RETURNING id INTO item;
    INSERT INTO public."Shop" ("itemId", "teamId") VALUES (item, f.team_id) RETURNING id INTO purchase;
    rejected := false;
    BEGIN PERFORM public.undo_ledger_entry('task', attempt, f.team_id); EXCEPTION WHEN raise_exception THEN rejected := true; END;
    ASSERT rejected, 'Insufficient remaining score must reject undo';
    ASSERT EXISTS (SELECT 1 FROM public."ActiveTasksLedger" WHERE id = attempt), 'Rejected undo must retain active attempt';
    ASSERT NOT EXISTS (SELECT 1 FROM public."TasksLedger" WHERE "reversesId" = attempt), 'Rejected undo must not leave a reversal';
    PERFORM public.undo_ledger_entry('shop', purchase, f.team_id);
    PERFORM public.undo_ledger_entry('task', attempt, f.team_id);
END;
$$;
DO $$
DECLARE f record; mini bigint; location_id bigint; effect bigint; unlock_item bigint; rewind_item bigint; boss_item bigint;
    unlock_purchase bigint; rewind_purchase bigint; boss_purchase bigint; failed bigint; success bigint; rejected boolean;
BEGIN
    SELECT * INTO f FROM fixture;
    SELECT "locationId" INTO location_id FROM public."Tasks" WHERE id = f.task_id;
    INSERT INTO public."Tasks" (text, solution, "gameId", "locationId", "isMiniBoss")
        VALUES ('Mini', 'answer', f.game_id, location_id, true) RETURNING id INTO mini;
    SELECT id INTO effect FROM public."ItemEffects" WHERE code = 'miniboss_unlock';
    INSERT INTO public."Items" (name, price, "gameId", "itemEffectId") VALUES ('Unlock', 0, f.game_id, effect) RETURNING id INTO unlock_item;
    SELECT id INTO effect FROM public."ItemEffects" WHERE code = 'miniboss_rewind';
    INSERT INTO public."Items" (name, price, "gameId", "itemEffectId") VALUES ('Rewind', 0, f.game_id, effect) RETURNING id INTO rewind_item;
    SELECT id INTO effect FROM public."ItemEffects" WHERE code = 'boss_location_unlock';
    INSERT INTO public."Items" (name, price, "gameId", "itemEffectId") VALUES ('Boss', 0, f.game_id, effect) RETURNING id INTO boss_item;
    INSERT INTO public."Shop" ("itemId", "teamId", "targetId") VALUES (unlock_item, f.team_id, mini) RETURNING id INTO unlock_purchase;
    INSERT INTO public."TasksLedger" ("taskId", "teamId", "isSuccess") VALUES (mini, f.team_id, false) RETURNING id INTO failed;
    INSERT INTO public."Shop" ("itemId", "teamId", "targetId") VALUES (rewind_item, f.team_id, mini) RETURNING id INTO rewind_purchase;
    INSERT INTO public."TasksLedger" ("taskId", "teamId", "isSuccess") VALUES (mini, f.team_id, true) RETURNING id INTO success;
    INSERT INTO public."Shop" ("itemId", "teamId") VALUES (boss_item, f.team_id) RETURNING id INTO boss_purchase;
    rejected := false;
    BEGIN PERFORM public.undo_ledger_entry('shop', unlock_purchase, f.team_id); EXCEPTION WHEN raise_exception THEN rejected := true; END;
    ASSERT rejected, 'An unlock used by attempts must not be undone';
    rejected := false;
    BEGIN PERFORM public.undo_ledger_entry('shop', rewind_purchase, f.team_id); EXCEPTION WHEN raise_exception THEN rejected := true; END;
    ASSERT rejected, 'A rewind used by a retry must not be undone';
    rejected := false;
    BEGIN PERFORM public.undo_ledger_entry('task', success, f.team_id); EXCEPTION WHEN raise_exception THEN rejected := true; END;
    ASSERT rejected, 'A miniboss success required by a boss unlock must not be undone';
    PERFORM public.undo_ledger_entry('shop', boss_purchase, f.team_id);
    PERFORM public.undo_ledger_entry('task', success, f.team_id);
    PERFORM public.undo_ledger_entry('shop', rewind_purchase, f.team_id);
    PERFORM public.undo_ledger_entry('task', failed, f.team_id);
    PERFORM public.undo_ledger_entry('shop', unlock_purchase, f.team_id);
    ASSERT NOT public.team_has_miniboss_unlock(f.team_id, mini), 'Undo must remove unlock effects';
    ASSERT NOT public.team_has_miniboss_success(f.team_id, mini), 'Undo must remove miniboss success';
END;
$$;
ROLLBACK;
