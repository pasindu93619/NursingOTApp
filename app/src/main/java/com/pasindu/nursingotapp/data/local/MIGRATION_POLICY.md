# NursingOTApp Room Migration Policy

This file is the implementation contract for future Room schema changes.

## Current schema

- Current Room version: 12
- Database name: `nursing_ot_app.db`
- Migration registry: `DatabaseMigrationRegistry.ALL_MIGRATIONS`
- Current migration: `MIGRATION_11_12`
- Destructive migration fallback: prohibited

## Required process for every schema change

1. Increase the Room database version by exactly one for a normal feature migration.
2. Add `MIGRATION_<oldVersion>_<newVersion>` to `AppDatabase`.
3. Register the migration in `DatabaseMigrationRegistry.ALL_MIGRATIONS`.
4. Keep all older migrations that may be required for an existing installation.
5. Update/export the Room schema snapshot when schema export is enabled.
6. Add or update migration tests for the new version.
7. Verify existing user data is preserved, especially profile, salary, financial, claim-period, OT and pay-sheet data.
8. Run `:app:compileDebugKotlin` and `test` before the migration is considered verified.

## 12 -> 13 approach

The next schema change must be implemented as `MIGRATION_12_13` only when a real persisted-schema change is required. The migration must perform the smallest SQL change necessary, preserve existing rows, and be registered before the new app version is shipped.

Do not create a placeholder migration merely to advance the version number. Do not use `fallbackToDestructiveMigration()` to bypass a missing migration.

## Offline-first rule

Room remains the local source of truth. A migration must never delete or recreate valid user tables merely to make a new schema compile. Any table replacement must explicitly copy compatible existing data and be covered by migration tests.

## Test strategy

At minimum, migration tests must verify:

- the expected source and destination versions are registered;
- the latest database version matches the registry;
- the migration can open an older supported database and reach the latest schema;
- existing profile data survives;
- salary data survives;
- financial/OT claim data survives;
- pay-sheet document metadata survives.

Where a migration changes a table, add a focused test for that table's existing rows before testing the latest schema.
