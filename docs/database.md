# Database Schema

The Supabase migrations in `supabase/migrations/` are the source of truth for the database schema. This document
explains the domain model, relationships, and app-level rules.

## Source of Truth

- `20260421115400_remote_schema.sql` is the baseline pulled from the existing remote database.
- Add new database changes with `supabase migration new <name>`.
- Apply pending remote migrations with `supabase db push`.

## Seed Data

- `supabase/seed.sql` contains dummy data for local development and application testing.
- Supabase applies it after migrations when running `supabase db reset`. (Make sure to have a docker daemon - in
  practice, Docker Desktop - running before running that)
- The seed uses fixed IDs in the `900001+` range and `ON CONFLICT` upserts, so it can be rerun locally without
  duplicating rows.
- The demo data includes games, locations, tasks, healing tasks, team assignments, teams, students, task ledgers,
  healing ledgers, shop items, and shop purchases.
- Several teams have unhealed failed `TasksLedger` rows so the healer screen has selectable failed tasks. A few failed
  rows are also already referenced by `HealingLedger` to test previously used healing tasks.

## Entity Model

See the [Supabase schema visualizer](https://supabase.com/dashboard/project/nnhvnqpczerqrofxkbki/database/schemas) (if
you have access to the project).

## Tables

### `Games`

The active game context. The app starts by selecting or creating a game, and later setup/gameplay operations must use
that selected `Games.id`.

Key columns:

- `id`: primary key
- `created_at`: created timestamp
- `name`: display name

Referenced by:

- `Locations.gameId`
- `Tasks.gameId`
- `TeamAssignment.gameId`

### `Locations`

Places or stations inside a game. Locations are directly scoped to one game.

Key columns:

- `id`: primary key
- `name`: display name
- `gameId`: references `Games.id`

### `Tasks`

Problems or challenges in a game. Tasks are directly scoped to one game and may also belong to a location.

Key columns:

- `id`: primary key
- `text`: task prompt
- `solution`: expected answer
- `isMiniBoss`: marks miniboss tasks
- `gameId`: references `Games.id`
- `locationId`: references `Locations.id`

### `HealingTasks`

Recovery challenges that can heal failed task attempts. Healing tasks are directly scoped to one game.

Key columns:

- `id`: primary key
- `text`: healing task prompt
- `solution`: expected answer
- `gameId`: references `Games.id`

### `TeamAssignment`

The saved team setup for a game. This is game-scoped so teams and students can be resolved through the selected game.

Key columns:

- `id`: primary key
- `baseTeamCounter`: planned team count
- `gameId`: references `Games.id`

### `Teams`

Teams produced by a team assignment.

Key columns:

- `id`: primary key
- `name`: display name
- `teamAssignmentId`: references `TeamAssignment.id`

### `Students`

Students assigned to teams.

Key columns:

- `id`: primary key
- `name`: display name
- `group`: group identifier
- `klass`: class identifier (i.e. grade)
- `teamId`: references `Teams.id`

### `TasksLedger`

History of task attempts.

Key columns:

- `id`: primary key
- `taskId`: references `Tasks.id`
- `teamId`: references `Teams.id`
- `userId`: acting user identifier
- `isSuccess`: attempt result

### `HealingLedger`

History of healing actions that connect a healing task to a failed task attempt.

Key columns:

- `id`: primary key
- `teamId`: references `Teams.id`
- `healingTaskId`: references `HealingTasks.id`
- `healedTasksLedgerId`: references `TasksLedger.id`
- `userId`: acting user identifier

### `ItemEffect`

Global reference data for shop item effects (i.e. location doubling, area doubling). The corresponding actions are
happening in triggers/edge functions, so that they are updatable without changing anything on frontend.

Key columns:

- `id`: primary key
- `description`: effect description

### `Item`

Global reference data for shop items.

Key columns:

- `id`: primary key
- `name`: display name
- `price`: item price
- `itemEffectId`: references `ItemEffect.id`

### `Shop`

Ledger of purchased shop items.

Key columns:

- `id`: primary key
- `itemId`: references `Item.id`
- `targetId`: purchase target identifier
- `userId`: acting user identifier

## Scoping Rules

- User-facing setup and gameplay data must be scoped by the selected `Games.id`.
- `Tasks`, `Locations`, and `TeamAssignment` are directly game-scoped.
- `Teams` and `Students` are game-scoped through `TeamAssignment`.
- Ledger rows are game-scoped through their referenced tasks or team-related records.
- `Item` and `ItemEffect` are global reference tables until gameplay requirements need per-game shop catalogs.
- Avoid UI-facing repository calls that read broad table data, such as `getAll()`, when a selected-game scoped method
  can be used.

## RLS

TODO

Row-level security is enabled on all public tables in the current schema.

Current temporary policies:

- `Games`: anonymous and authenticated users can read and create rows.
- `TeamAssignment`: anonymous and authenticated users can read and create rows when `gameId` is present.

These policies match the current app state where authentication is disabled/stubbed in navigation. Before enabling real
multi-user access, replace the public policies with authenticated access or explicit game membership rules.
