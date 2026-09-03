# Room Migration Framework

This document records the Phase 1.2 migration policy for NursingOTApp.

## Current schema

Current Room database version: 12.

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

Do not create a dummy 12 -> 13 migration solely to satisfy versioning. The first real feature that requires a schema change must provide the migration and its tests together.

## Safety rules

Room remains the local source of truth.

Destructive migration fallback must remain disabled.

Never delete or rewrite an existing migration merely because the application has advanced to a newer schema version.
