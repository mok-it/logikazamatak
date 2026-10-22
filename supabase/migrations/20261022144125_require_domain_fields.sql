-- No values are inferred, changed or deleted. The entire migration is one atomic block.
DO $$
DECLARE
    required_fields constant jsonb := '{"Games": ["name"], "Locations": ["name", "gameId"], "Tasks": ["text", "solution", "isMiniBoss", "gameId"], "HealingTasks": ["text", "gameId"], "TeamAssignment": ["baseTeamCounter", "gameId"], "Teams": ["name", "teamAssignmentId"], "Students": ["name", "group", "teamId"], "Items": ["name", "price", "itemEffectId", "gameId"], "TasksLedger": ["teamId", "isSuccess"], "HealingLedger": ["teamId", "healingTaskId", "healedTasksLedgerId"], "Shop": ["itemId", "teamId"]}'::jsonb;
    entry record;
    field_name text;
    null_count bigint;
    problems text[] := '{}';
BEGIN
    -- Prevent writes between the preflight and constraint installation.
    FOR entry IN SELECT key AS table_name FROM jsonb_each(required_fields) ORDER BY key LOOP
        EXECUTE format('LOCK TABLE public.%I IN ACCESS EXCLUSIVE MODE', entry.table_name);
    END LOOP;
    FOR entry IN SELECT key AS table_name, value AS fields FROM jsonb_each(required_fields) LOOP
        FOR field_name IN SELECT jsonb_array_elements_text(entry.fields) LOOP
            EXECUTE format('SELECT count(*) FROM public.%I WHERE %I IS NULL', entry.table_name, field_name)
                INTO null_count;
            IF null_count > 0 THEN
                problems := array_append(problems, format('%s.%s: %s null row(s)', entry.table_name, field_name, null_count));
            END IF;
        END LOOP;
    END LOOP;
    IF cardinality(problems) > 0 THEN
        RAISE EXCEPTION USING
            ERRCODE = '23502',
            MESSAGE = 'Required domain fields contain null values; no schema changes were applied.',
            DETAIL = array_to_string(problems, E'\n'),
            HINT = 'Review and repair the listed rows from authoritative data, then rerun this migration. Do not invent IDs, names, prices or results.';
    END IF;
    FOR entry IN SELECT key AS table_name, value AS fields FROM jsonb_each(required_fields) LOOP
        FOR field_name IN SELECT jsonb_array_elements_text(entry.fields) LOOP
            EXECUTE format('ALTER TABLE public.%I ALTER COLUMN %I SET NOT NULL', entry.table_name, field_name);
        END LOOP;
    END LOOP;
    ALTER TABLE public."Tasks" ALTER COLUMN "isMiniBoss" SET DEFAULT false;
END;
$$;
