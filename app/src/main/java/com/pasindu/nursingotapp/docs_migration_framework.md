# Room Migration Framework

This document records the Phase 1.2 migration policy for NursingOTApp.

## Current schema

Current Room database version: 12.

The verified v12 schema is documented in `docs_database_schema.md`. That document is the reference for the current persisted data model.

## Naming convention

Every future migration must use:

`MIGRATION_<oldVersion>_<newVersion>`

Examples:

`MIGRATION_12_13`

`MIGRATION_13_14`

## Registry policy

`DatabaseMigrationRegistry.ALL_MIGRATIONS` is the single source of truth for migrations used by database builders.

Do not maintain independent migration arrays in individual database providers.

## Test policy

Every schema-changing release must provide a migration test that:

1. creates the relevant legacy database version;
2. inserts representative existing user data;
3. runs the migration;
4. validates the migrated schema;
5. validates preservation of existing data;
6. validates new columns, tables, indexes, and constraints.

## Version 12 baseline

Version 12 contains the existing nurse productivity, finance, salary-step, and pay-sheet data model. The next real schema change will be migration 12 -> 13.

## Future 12 -> 13 approach

Do not create a dummy `MIGRATION_12_13` solely to satisfy versioning.

When the first real Phase 1.3+ feature requires a Room schema change:

1. inspect the current v12 schema and the exact feature data requirement;
2. define the smallest additive schema change required;
3. implement `MIGRATION_12_13` as a non-destructive migration from the verified v12 schema;
4. update the Room database version to 13 only with that real schema change;
5. register the migration in `DatabaseMigrationRegistry.ALL_MIGRATIONS`;
6. add an Android migration test using a real v12 schema fixture;
7. insert representative existing profile, duty/claim, financial, salary and pay-sheet data before migration;
8. verify every existing record remains readable after migration;
9. verify the new v13 table/column/index/constraint exactly matches the Room entity schema;
10. run the full unit-test suite and connected Android migration tests before the checklist item is considered verified.

If the future change can be implemented without changing the persisted Room schema, do not increment the database version and do not create a migration.

The existing v1 -> v2 verification remains deferred because the repository does not contain a verifiable v1 schema artifact. It must not be reconstructed or guessed later; verification should resume only when an authentic v1 database/schema artifact is available.

## Safety rules

Room remains the local source of truth.

Destructive migration fallback must remain disabled.

Never delete or rewrite an existing migration merely because the application has advanced to a newer schema version.

Never use `fallbackToDestructiveMigration()` to bypass a missing future migration.
