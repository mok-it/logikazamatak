-- Run with psql -v ON_ERROR_STOP=1 -f supabase/tests/team_composition.sql against a migrated local DB.
BEGIN;
SET LOCAL ROLE anon;
DO $$
DECLARE
    game_id bigint;
    other_game_id bigint;
    team_id bigint;
    student_id bigint;
    task_id bigint;
    roster jsonb;
    rejected boolean;
BEGIN
    INSERT INTO public."Games" (name) VALUES ('Roster regression') RETURNING id INTO game_id;
    PERFORM public.save_team_composition(game_id, 1,
        '[{"name":"Red","group":"A","students":[{"name":"Anna"}]}]');
    SELECT t.id, s.id INTO team_id, student_id FROM public."Teams" t
        JOIN public."Students" s ON s."teamId" = t.id
        JOIN public."TeamAssignment" a ON a.id = t."teamAssignmentId"
        WHERE a."gameId" = game_id;
    ASSERT team_id IS NOT NULL AND student_id IS NOT NULL, 'Anonymous save must persist a readable roster';
    UPDATE public."Teams" SET "additionalScoreAwarded" = 3 WHERE id = team_id;
    roster := jsonb_build_array(jsonb_build_object(
        'id', team_id, 'name', 'Renamed', 'group', 'B',
        'students', jsonb_build_array(jsonb_build_object('id', student_id, 'name', 'Anna edited'))
    ));
    PERFORM public.save_team_composition(game_id, 2, roster);
    ASSERT EXISTS (SELECT 1 FROM public."Teams" WHERE id = team_id AND name = 'Renamed' AND "additionalScoreAwarded" = 3),
        'Editing must retain team identity and score';
    ASSERT EXISTS (SELECT 1 FROM public."Students" WHERE id = student_id AND name = 'Anna edited' AND "group" = 'B'),
        'Editing must retain student identity and category';

    PERFORM public.save_team_composition(game_id, 2, roster ||
        '[{"name":"Temporary","group":"C","students":[{"name":"New"}]}]'::jsonb);
    PERFORM public.save_team_composition(game_id, 2, roster);
    ASSERT (SELECT count(*) = 1 FROM public."Teams" WHERE "teamAssignmentId" IN (
        SELECT id FROM public."TeamAssignment" WHERE "gameId" = game_id)), 'Unused teams must be removable';

    rejected := false;
    BEGIN
        PERFORM public.save_team_composition(game_id, 99, roster ||
            '[{"name":"Invalid","group":"A","students":[{"name":""}]}]'::jsonb);
    EXCEPTION WHEN raise_exception THEN rejected := true;
    END;
    ASSERT rejected, 'Invalid nested data must be rejected';
    ASSERT (SELECT "baseTeamCounter" = 2 FROM public."TeamAssignment" WHERE "gameId" = game_id),
        'Validation failure must roll back assignment updates';
    ASSERT (SELECT count(*) = 1 FROM public."Teams" WHERE "teamAssignmentId" IN (
        SELECT id FROM public."TeamAssignment" WHERE "gameId" = game_id)), 'Failure must roll back newly inserted teams';

    INSERT INTO public."Games" (name) VALUES ('Other game') RETURNING id INTO other_game_id;
    rejected := false;
    BEGIN
        PERFORM public.save_team_composition(other_game_id, 1, roster);
    EXCEPTION WHEN raise_exception THEN rejected := true;
    END;
    ASSERT rejected, 'A roster must not steal a team from another game';
    ASSERT NOT EXISTS (SELECT 1 FROM public."TeamAssignment" WHERE "gameId" = other_game_id),
        'Rejected cross-game save must roll back the new assignment';

    INSERT INTO public."Tasks" (text, solution, "gameId", "isMiniBoss")
        VALUES ('Test', 'answer', game_id, false) RETURNING id INTO task_id;
    INSERT INTO public."TasksLedger" ("taskId", "teamId", "isSuccess") VALUES (task_id, team_id, true);
    PERFORM public.save_team_composition(game_id, 2, roster);
    rejected := false;
    BEGIN
        PERFORM public.save_team_composition(game_id, 1,
            '[{"name":"Replacement","group":"A","students":[{"name":"New"}]}]');
    EXCEPTION WHEN raise_exception THEN rejected := true;
    END;
    ASSERT rejected, 'Import must not replace a team with game history';
    ASSERT EXISTS (SELECT 1 FROM public."TasksLedger" WHERE "teamId" = team_id), 'Game history must survive';
    ASSERT EXISTS (SELECT 1 FROM public."Students" WHERE id = student_id), 'Rejected replacement must retain students';
END;
$$;
ROLLBACK;
