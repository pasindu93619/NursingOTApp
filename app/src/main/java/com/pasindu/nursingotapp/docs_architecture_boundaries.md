# Nursing Super App Architecture Boundaries

This document establishes the Phase 1.4 repository/domain/data/UI boundary for the current NursingOTApp codebase. It is an incremental boundary contract, not a request for a broad rewrite.

## Layers

### data
Owns persistence and external data access.
- `data/local`: Room database, entities, DAOs, migrations and local seeders.
- `data/repository`: repository implementations that coordinate data access.
- `data/paysheet`: pay-sheet/document data handling.
- Data layer may depend on Room/Android persistence APIs and domain models where required.
- UI code must not access Room DAOs or `AppDatabase` directly.

### domain
Owns application rules and reusable business/clinical logic.
- `domain/calculation`: deterministic calculation engines.
- `domain/ot`: OT/duty calculation rules.
- `domain/usecase`: application operations that coordinate repositories and domain rules.
- `domain/model`: domain-facing models and decision objects.
- Domain logic must not depend on Compose UI, Activities, Screens or ViewModels.
- Financial and clinical rules remain deterministic and authoritative here.

### ui
Owns presentation only.
- Screens/Composables render state and dispatch user actions.
- ViewModels own screen state and lifecycle-aware coroutine work.
- ViewModels call use cases/repositories through injected dependencies.
- Composables should not contain authoritative financial or clinical calculations, Room operations, or repository construction.

### di
Owns dependency wiring only.
- Hilt modules provide the database, DAOs, repositories and use cases.
- DI modules must not contain business rules.
- `DatabaseModule` remains the single Hilt construction point for `AppDatabase` and preserves the existing salary-table seeding behavior.

## Dependency direction

`UI -> ViewModel -> UseCase -> Repository -> DAO -> Room`

Domain calculation engines may be called by use cases or repositories as appropriate, but must remain independent of UI presentation.

`UI -> data/local` and `UI -> AppDatabase` are prohibited for new code.
`domain -> ui` is prohibited.
`di -> business rules` is prohibited.

## Existing-code preservation

The current database is Room/offline-first and remains the local source of truth. Existing OT/duty calculations, financial calculations, salary-step data, pay-sheet history, clinical planning, Knowledge Hub/CPD, wellness and Nurse Command Center behavior must be preserved while boundaries are tightened incrementally.

Existing compatibility code may remain temporarily while callers are migrated. Removal requires verification that no working feature still depends on it.

## Incremental refactoring rule

For each future Phase 1.4 change:
1. Inspect the current implementation on the active branch.
2. Identify one concrete boundary violation or inconsistency.
3. Make the smallest additive/refactoring change that fixes that issue.
4. Run unit tests and the relevant Android tests.
5. Only then remove obsolete wiring if the verified callers no longer need it.

No new patient/clinical cloud storage is introduced by this architecture contract. Security requirements remain governed by Phase 7 before patient data is added.
