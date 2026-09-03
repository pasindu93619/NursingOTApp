# Architecture Refactor Progress

## Phase 1.4 — current bounded slice

This branch introduces the first domain boundary around the existing profile, compensation, pay-rate, salary-step, and daily-entry functionality used by `NursingViewModel`.

### Completed in this slice

- Added profile/finance domain use cases for observing and saving profile-related data.
- Added salary-step matching and 2027 day-rate application use cases.
- Added daily-entry observation, save, and date lookup use cases.
- Registered those use cases in Hilt through `UseCaseModule`.
- Updated `NursingViewModel` so its profile/pay-rate/salary-step/daily-entry orchestration goes through domain use cases instead of directly calling DAOs.
- Kept Room entities and DAOs in the data layer.
- Preserved the existing `NurseCommandCenterRepository` boundary for multi-source dashboard data.
- Added focused unit tests for the extracted salary/pay-rate use cases.

### Intentional scope limit

No broad screen rewrite, database change, or new feature is introduced by this slice. Existing UI behavior and deterministic calculation logic remain unchanged.

### Next architectural targets

Continue migrating remaining ViewModels/screens that still access `DatabaseProvider` or instantiate their own ViewModels. Prioritize one module at a time and keep calculation/business rules out of Composables.
