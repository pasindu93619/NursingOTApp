# NursingOTApp Architecture Boundaries

Phase 1.4 foundation for incremental cleanup.

## Intended dependency direction

UI -> ViewModel -> Domain use case -> Repository/data gateway -> DAO -> Room

The UI layer should observe state and emit user intents. ViewModels coordinate state and lifecycle. Domain use cases own feature-level orchestration that is independent of Compose. Repositories/data gateways isolate persistence details. DAOs remain the Room-specific persistence interface.

## Current approach

Existing working modules are being migrated incrementally. Hilt is already available and database/DAO dependencies are injected. Direct DAO access is being removed from the largest ViewModels first by introducing small domain use cases.

## Rules

1. Do not move deterministic clinical, OT, or financial rules into Composables.
2. Prefer domain use cases for write operations and feature-level queries used by ViewModels.
3. Keep Room entities in the data layer; do not expose them to new UI code unless migration of an existing screen makes that temporarily necessary.
4. Keep repositories responsible for combining multiple data sources and hiding persistence details.
5. Use StateFlow from ViewModels for UI state.
6. Preserve existing behavior while refactoring.
7. Make one bounded architectural change at a time and verify with Gradle tests/build.

## Phase 1.4 sequence

The first bounded migration is the profile/finance/daily-entry slice used by NursingViewModel. The ViewModel now depends on domain use cases rather than directly orchestrating its profile, compensation, salary-step, pay-rate, and daily-entry DAO operations.

Next refactors should follow the same pattern for remaining modules, starting with screens that still instantiate ViewModels or access DatabaseProvider directly.
