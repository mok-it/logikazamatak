-- Run BEFORE the require_domain_fields migration on a disposable database.
\set ON_ERROR_STOP on
BEGIN;
INSERT INTO public."Games" (name) VALUES (NULL);
SAVEPOINT before_migration;
\set ON_ERROR_STOP off
\ir ../migrations/20261022144125_require_domain_fields.sql
\set observed_state :SQLSTATE
\set ON_ERROR_STOP on
ROLLBACK TO SAVEPOINT before_migration;
SELECT :'observed_state' = '23502' AS rejected \gset
\if :rejected
DO $$
BEGIN
    ASSERT EXISTS (SELECT 1 FROM public."Games" WHERE name IS NULL), 'Preflight must preserve legacy rows';
    ASSERT (SELECT is_nullable = 'YES' FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'Items' AND column_name = 'itemEffectId'),
        'Failed migration must not install partial constraints';
END;
$$;
\else
\echo 'Expected preflight not_null_violation was not raised'
\quit 1
\endif
ROLLBACK;
