# CPD Tracking Improvement

Phase 3.5 Knowledge / CPD additive UI improvement.

- Preserves existing CpdLogEntity and Room persistence.
- Shows annual CPD progress against the existing 30-point target.
- Shows activities completed and points recorded.
- Shows remaining points, or a target-reached state.
- Shows the recorded date for each CPD activity.
- Preserves the existing Log CPD flow and existing Knowledge Hub circular search, category filtering, and bookmarks.
- No new dependency and no database migration.

Local verification required before checklist completion:
- `./gradlew :app:compileDebugKotlin`
- `./gradlew test`
- Manual CPD screen verification after pulling the branch.
