DROP FUNCTION public.save_team_composition(bigint, bigint, jsonb);

CREATE FUNCTION public.save_team_composition(
    p_game_id bigint,
    p_base_team_counter bigint,
    p_teams jsonb,
    p_assignment_id bigint DEFAULT NULL
) RETURNS void
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = ''
AS $$
DECLARE
    assignment_id bigint;
    team_data jsonb;
    student_data jsonb;
    team_id bigint;
    student_id bigint;
    kept_team_ids bigint[] := '{}';
    kept_student_ids bigint[];
BEGIN
    IF p_base_team_counter IS NULL OR p_base_team_counter < 1
       OR jsonb_typeof(p_teams) IS DISTINCT FROM 'array'
       OR jsonb_array_length(p_teams) = 0 THEN
        RAISE EXCEPTION 'Adj meg pozitív alap csapatszámot és legalább egy csapatot.';
    END IF;
    -- Serialize roster saves for the same game, including its first assignment.
    PERFORM pg_catalog.pg_advisory_xact_lock(p_game_id);
    IF NOT EXISTS (SELECT 1 FROM public."Games" WHERE id = p_game_id) THEN
        RAISE EXCEPTION 'A játék nem található.';
    END IF;
    IF p_assignment_id IS NULL THEN
        SELECT id INTO assignment_id FROM public."TeamAssignment"
            WHERE "gameId" = p_game_id ORDER BY id DESC LIMIT 1;
    ELSE
        SELECT id INTO assignment_id FROM public."TeamAssignment"
            WHERE "gameId" = p_game_id AND id = p_assignment_id;
        IF assignment_id IS NULL THEN
            RAISE EXCEPTION 'A beosztás nem ehhez a játékhoz tartozik.';
        END IF;
    END IF;
    IF assignment_id IS NULL THEN
        INSERT INTO public."TeamAssignment" ("gameId", "baseTeamCounter")
            VALUES (p_game_id, p_base_team_counter) RETURNING id INTO assignment_id;
    ELSE
        UPDATE public."TeamAssignment" SET "baseTeamCounter" = p_base_team_counter
            WHERE id = assignment_id;
    END IF;

    FOR team_data IN SELECT value FROM jsonb_array_elements(p_teams) LOOP
        IF coalesce(btrim(team_data->>'name'), '') = ''
           OR coalesce(btrim(team_data->>'group'), '') = ''
           OR jsonb_typeof(team_data->'students') IS DISTINCT FROM 'array'
           OR jsonb_array_length(team_data->'students') = 0 THEN
            RAISE EXCEPTION 'Minden csapatnak legyen neve, csoportja és legalább egy tanulója.';
        END IF;
        team_id := (team_data->>'id')::bigint;
        IF team_id IS NULL THEN
            INSERT INTO public."Teams" (name, "teamAssignmentId")
                VALUES (btrim(team_data->>'name'), assignment_id) RETURNING id INTO team_id;
        ELSE
            IF team_id = ANY(kept_team_ids) OR NOT EXISTS (
                SELECT 1 FROM public."Teams" WHERE id = team_id AND "teamAssignmentId" = assignment_id
            ) THEN
                RAISE EXCEPTION 'A csapat nem ehhez a beosztáshoz tartozik, vagy többször szerepel.';
            END IF;
            UPDATE public."Teams" SET name = btrim(team_data->>'name') WHERE id = team_id;
        END IF;
        kept_team_ids := array_append(kept_team_ids, team_id);
        kept_student_ids := '{}';
        FOR student_data IN SELECT value FROM jsonb_array_elements(team_data->'students') LOOP
            IF coalesce(btrim(student_data->>'name'), '') = '' THEN
                RAISE EXCEPTION 'A tanuló neve nem lehet üres.';
            END IF;
            student_id := (student_data->>'id')::bigint;
            IF student_id IS NULL THEN
                INSERT INTO public."Students" (name, "group", "teamId")
                    VALUES (btrim(student_data->>'name'), btrim(team_data->>'group'), team_id)
                    RETURNING id INTO student_id;
            ELSE
                IF student_id = ANY(kept_student_ids) OR NOT EXISTS (
                    SELECT 1 FROM public."Students" WHERE id = student_id AND "teamId" = team_id
                ) THEN
                    RAISE EXCEPTION 'A tanuló nem ehhez a csapathoz tartozik, vagy többször szerepel.';
                END IF;
                UPDATE public."Students" SET name = btrim(student_data->>'name'),
                    "group" = btrim(team_data->>'group') WHERE id = student_id;
            END IF;
            kept_student_ids := array_append(kept_student_ids, student_id);
        END LOOP;
        DELETE FROM public."Students" WHERE "teamId" = team_id AND NOT (id = ANY(kept_student_ids));
    END LOOP;

    IF EXISTS (
        SELECT 1 FROM public."Teams" t WHERE t."teamAssignmentId" = assignment_id
        AND NOT (t.id = ANY(kept_team_ids)) AND (
            EXISTS (SELECT 1 FROM public."TasksLedger" WHERE "teamId" = t.id)
            OR EXISTS (SELECT 1 FROM public."HealingLedger" WHERE "teamId" = t.id)
            OR EXISTS (SELECT 1 FROM public."Shop" WHERE "teamId" = t.id)
        )
    ) THEN
        RAISE EXCEPTION 'Már játszó csapat nem törölhető vagy cserélhető le importtal. Szerkeszd a meglévő csapatot.';
    END IF;
    DELETE FROM public."Students" WHERE "teamId" IN (
        SELECT id FROM public."Teams" WHERE "teamAssignmentId" = assignment_id
        AND NOT (id = ANY(kept_team_ids))
    );
    DELETE FROM public."Teams" WHERE "teamAssignmentId" = assignment_id
        AND NOT (id = ANY(kept_team_ids));
END;
$$;
REVOKE ALL ON FUNCTION public.save_team_composition(bigint, bigint, jsonb, bigint) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.save_team_composition(bigint, bigint, jsonb, bigint) TO anon, authenticated;
