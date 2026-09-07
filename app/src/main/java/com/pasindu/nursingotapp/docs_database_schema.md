# NursingOTApp Room Database Schema

This document records the verified Room database structure for the current Nursing Super App database baseline.

## Current baseline

- Database class: `AppDatabase`
- Current Room version: `12`
- Schema export: enabled with `exportSchema = true`
- Local Room database is the offline-first source of truth.
- Destructive migration fallback is not permitted.
- The exported Room schema JSON for version 12 is the authoritative machine-readable schema snapshot.

## Version 12 entity/table inventory

`AppDatabase` currently registers these 11 Room entities:

| Room entity | SQLite table | Main responsibility |
|---|---|---|
| `ProfileEntity` | `profile` | Master nurse profile and current basic salary data |
| `ClaimPeriodEntity` | `claim_period` | OT/claim period information |
| `DailyEntryEntity` | `daily_entry` | Daily duty/OT entry information |
| `FinancialRecordEntity` | `financial_records` | Monthly financial and salary records |
| `IsbarNoteEntity` | `isbar_notes` | Local ISBAR clinical notes |
| `ClinicalTaskEntity` | `clinical_tasks` | Clinical task/reminder data |
| `CpdLogEntity` | `cpd_logs` | CPD/learning records |
| `PayRateSettingsEntity` | `pay_rate_settings` | User-configured OT/PH/DO rate settings |
| `ProfileCompensationEntity` | `profile_compensation` | Compensation/allowance/deduction data associated with the profile |
| `SalaryStep2027Entity` | `salary_steps_2027` | 2027 salary-step reference data |
| `PaySheetDocumentEntity` | `pay_sheet_documents` | Locally indexed pay-sheet documents |

## Verified table structures introduced/managed directly by migrations

### `financial_records`

Current columns are:

- `id` INTEGER primary key, auto-generated
- `recordMonth` TEXT
- `timestamp` INTEGER
- `basicSalary` REAL
- `otRate` REAL
- `otHours` REAL
- `phDays` REAL
- `doDays` REAL
- `wopDeduction` REAL
- `apitTaxAmount` REAL
- `loanDeduction` REAL
- `otherDeductions` REAL
- `totalHoursWorked` REAL
- `grossSalary` REAL
- `netSalary` REAL

### `pay_rate_settings`

Current migration-created columns are:

- `id` INTEGER primary key
- `otRate` REAL
- `phRate` REAL
- `doRate` REAL
- `rateSource` TEXT
- `basisSalary2027` REAL nullable
- `updatedAt` INTEGER

### `profile_compensation`

Current migration-created columns are:

- `id` INTEGER primary key
- `riskAllowance` REAL
- `claAllowance` REAL
- `additionalAllowancesTotal` REAL
- `totalDeductions` REAL
- `updatedAt` INTEGER

### `salary_steps_2027`

Current version 12 structure is:

- `id` INTEGER primary key, auto-generated
- `grade` TEXT
- `salaryStep` INTEGER
- `currentBasicSalary2026` REAL
- `basicSalary2027` REAL
- `effectiveFrom` TEXT
- `sourceLabel` TEXT

### `pay_sheet_documents`

Current migration-created columns are:

- `id` INTEGER primary key, auto-generated
- `monthKey` TEXT
- `displayMonth` TEXT
- `filePath` TEXT
- `fileSizeBytes` INTEGER
- `sha256` TEXT
- `createdAt` INTEGER
- `updatedAt` INTEGER

A unique index exists on `monthKey`.

### `isbar_notes`

Migration-created columns are:

- `id` INTEGER primary key, auto-generated
- `patientIdentifier` TEXT
- `identification` TEXT
- `situation` TEXT
- `background` TEXT
- `assessment` TEXT
- `recommendation` TEXT
- `timestamp` INTEGER

### `clinical_tasks`

Migration-created columns are:

- `id` INTEGER primary key, auto-generated
- `taskName` TEXT
- `description` TEXT
- `priority` TEXT
- `triggerTime` INTEGER
- `isCompleted` INTEGER
- `bypassDnd` INTEGER

### `cpd_logs`

Migration-created columns are:

- `id` INTEGER primary key, auto-generated
- `seminarTitle` TEXT
- `date` INTEGER
- `earnedPoints` INTEGER
- `speakerOrInstitution` TEXT
- `notes` TEXT

## Profile safety boundary

`ProfileEntity` stores the nurse's current basic salary. Salary-step/policy data are kept separately so policy changes do not overwrite the nurse's actual current pay. `otRate` remains in the profile for legacy compatibility; active rate settings are held by `PayRateSettingsEntity`.

## Migration boundary

The current migration chain reaches version 12 through the registered migrations in `DatabaseMigrationRegistry.ALL_MIGRATIONS`.

The next real schema change must be implemented as a new migration (for example `MIGRATION_12_13`) together with a migration test. Do not create a dummy migration just to increase the version number.

Historical v1 schema is not documented here because no verifiable v1 Room schema artifact exists in the repository history. v1 -> v2 verification therefore remains a future/deferred validation item and must not be represented as already tested.

## Source files

- `data/local/AppDatabase.kt` - Room entity registration and migration definitions
- `data/local/DatabaseMigrationRegistry.kt` - registered migration chain
- `data/local/entity/*` - current entity definitions
- `app/schemas/com.pasindu.nursingotapp.data.local.AppDatabase/12.json` - exported Room schema snapshot for version 12
- `docs_migration_framework.md` - migration naming, testing, and safety policy

## Change rule

Any future schema change must update the entity/schema, add the corresponding migration, add/extend migration tests, and update this document when the verified schema changes. No feature may bypass the migration process.
