# Architecture Refactor Progress

## Phase 1.4 — bounded slices

This phase introduces domain boundaries incrementally around existing nursing OT functionality without changing the established calculation rules or replacing working infrastructure.

### Completed

- Profile, compensation, pay-rate, salary-step, and daily-entry operations used by `NursingViewModel` are delegated through domain use cases and registered with Hilt.
- `ClaimPeriodScreen` no longer accesses `DatabaseProvider` or DAOs directly; claim-period observation, creation, deletion, and history-clearing operations are delegated through `ClaimPeriodViewModel` and domain use cases.
- Claim-period deletion preserves the existing data-integrity order: daily entries are deleted before the parent claim period.
- Focused unit tests cover extracted profile/pay-rate/salary-step and claim-period behavior.
- `NurseCommandCenterRepository` remains the boundary for its multi-source dashboard data.

### Verification

The bounded claim-period architecture slice has been verified locally with:

- `./gradlew test` — BUILD SUCCESSFUL
- `./gradlew assembleDebug` — BUILD SUCCESSFUL

### Intentional scope limit

No Room schema version change, destructive migration, broad screen rewrite, or new product feature is introduced by these slices. Existing UI behavior and deterministic OT/business calculation logic remain unchanged.

### Next architectural targets

Continue migrating remaining ViewModels/screens that still access `DatabaseProvider` or instantiate ViewModels directly. Prioritize one module at a time, standardize state/error handling, and keep business and clinical rules out of Composables.
