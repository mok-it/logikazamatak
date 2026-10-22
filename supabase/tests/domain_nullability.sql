-- Run after all migrations and seed with psql -v ON_ERROR_STOP=1.
BEGIN;
DO $$
DECLARE mismatch text; game_id bigint; task_id bigint; rejected boolean;
BEGIN
    SELECT string_agg(expected.table_name || '.' || expected.column_name, ', ') INTO mismatch
    FROM (VALUES
        ('Games', 'name', false),
        ('Locations', 'name', false),
        ('Locations', 'gameId', false),
        ('Tasks', 'text', false),
        ('Tasks', 'solution', false),
        ('Tasks', 'isMiniBoss', false),
        ('Tasks', 'gameId', false),
        ('HealingTasks', 'text', false),
        ('HealingTasks', 'gameId', false),
        ('TeamAssignment', 'baseTeamCounter', false),
        ('TeamAssignment', 'gameId', false),
        ('Teams', 'name', false),
        ('Teams', 'teamAssignmentId', false),
        ('Students', 'name', false),
        ('Students', 'group', false),
        ('Students', 'teamId', false),
        ('Items', 'name', false),
        ('Items', 'price', false),
        ('Items', 'itemEffectId', false),
        ('Items', 'gameId', false),
        ('TasksLedger', 'teamId', false),
        ('TasksLedger', 'isSuccess', false),
        ('HealingLedger', 'teamId', false),
        ('HealingLedger', 'healingTaskId', false),
        ('HealingLedger', 'healedTasksLedgerId', false),
        ('Shop', 'itemId', false),
        ('Shop', 'teamId', false),
        ('Tasks', 'locationId', true),
        ('HealingTasks', 'solution', true),
        ('Items', 'maxPerTeam', true),
        ('ItemEffects', 'description', true),
        ('Shop', 'targetId', true),
        ('Shop', 'userId', true),
        ('TasksLedger', 'userId', true),
        ('TasksLedger', 'bonusSourceShopId', true),
        ('TasksLedger', 'bonusSourceTasksLedgerId', true),
        ('HealingLedger', 'userId', true)
    ) expected(table_name, column_name, nullable)
    LEFT JOIN information_schema.columns c ON c.table_schema = 'public'
        AND c.table_name = expected.table_name AND c.column_name = expected.column_name
    WHERE c.column_name IS NULL OR (c.is_nullable = 'YES') IS DISTINCT FROM expected.nullable;
    ASSERT mismatch IS NULL, 'Unexpected nullability: ' || mismatch;

    INSERT INTO public."Games" (name) VALUES ('Nullability regression') RETURNING id INTO game_id;
    -- Location is optional; omitted miniboss flags default to false.
    INSERT INTO public."Tasks" (text, solution, "gameId") VALUES ('Task', 'Answer', game_id) RETURNING id INTO task_id;
    ASSERT (SELECT "isMiniBoss" IS FALSE AND "locationId" IS NULL FROM public."Tasks" WHERE id = task_id);
    -- Healing can be a manually judged activity without a textual answer.
    INSERT INTO public."HealingTasks" (text, "gameId") VALUES ('Sing a song', game_id);
    rejected := false;
    BEGIN
        INSERT INTO public."Items" (name, price, "gameId") VALUES ('No effect', 1, game_id);
    EXCEPTION WHEN not_null_violation THEN rejected := true;
    END;
    ASSERT rejected, 'Items must require an effect';
    rejected := false;
    BEGIN UPDATE public."Tasks" SET "solution" = NULL WHERE id = task_id;
    EXCEPTION WHEN not_null_violation THEN rejected := true;
    END;
    ASSERT rejected, 'Updates must not clear required answers';
END;
$$;
ROLLBACK;
