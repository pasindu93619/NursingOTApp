# NursingOTApp — Batch 4 Safe Test Preflight & Isolation Strategy

**Document ID:** `BATCH-4-PREFLIGHT-2026-11-06`  
**Execution Date:** 2026-11-06  
**Target Device:** Redmi Note 14 5G (`24094RAD4G`)  
**Operating System:** Android 16 / HyperOS  
**Package Under Protection:** `com.pasindu.nursingotapp`  
**Proposed Verification Package:** `com.pasindu.nursingotapp.verification`  
**Auditor / Assistant:** Antigravity UI Automation Agent  

---

## Preflight Verdict Summary

| Preflight Inspection Area | Current Architecture / State | Preflight Verdict | Notes / Mitigations |
| :--- | :--- | :---: | :--- |
| **Git Working Tree & Branch** | Branch `fix/mutual-transfer-test-polling-regressions`, HEAD `cb1d47b` | **PASS** | Read-only inspection verified; working tree changes from Batches 1–3 fully preserved |
| **Room Database Architecture** | Singleton `nursing_ot_app.db` via Hilt [`DatabaseModule.kt`](file:///c:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/di/DatabaseModule.kt#L17-L36) | **PASS** | Sandboxed to Linux application UID; distinct `applicationId` guarantees separate SQLite file |
| **ISBAR Data & Use Cases** | [`IsbarNoteEntity.kt`](file:///c:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/data/local/entity/IsbarNoteEntity.kt), [`ClinicalPlanningDao.kt`](file:///c:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/data/local/dao/ClinicalPlanningDao.kt), [`ClinicalPlanningUseCases.kt`](file:///c:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/domain/usecase/ClinicalPlanningUseCases.kt) | **PASS** | Complete CRUD use cases exist (`Add`, `Update`, `Delete`, `Observe`, `Purge`) |
| **Clinical Tasks Architecture** | [`ClinicalTaskEntity.kt`](file:///c:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/data/local/entity/ClinicalTaskEntity.kt), [`ClinicalPlanningDao.kt`](file:///c:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/data/local/dao/ClinicalPlanningDao.kt) | **PASS** | Complete CRUD use cases exist (`Add`, `Update`, `Delete`, `SetCompleted`, `Observe`) |
| **ISBAR 48h Auto-Purge** | [`PurgeOldIsbarNotesUseCase.kt`](file:///c:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/domain/usecase/ClinicalPlanningUseCases.kt#L94-L104) | **PASS** | Purge logic active with 48h retention cutoff (`timestamp < now - 48h`); UI button in TopAppBar |
| **PDF Generation Pipeline** | [`PdfGenerator.kt`](file:///c:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/ui/otforms/PdfGenerator.kt), [`WeeklyOtCalculator.kt`](file:///c:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/domain/ot/WeeklyOtCalculator.kt), [`PdfPreviewDialog.kt`](file:///c:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/ui/screens/PdfPreviewDialog.kt) | **PASS** | Generates to `cacheDir/pdf/OT_Form_Month.pdf`; renders via Android `PdfRenderer` |
| **UI Automation Driver** | `scratch/ui_automator_client.py` (UI Automator 2.0 on port 6790) | **PASS** | Physical device connected over ADB (`10.150.55.191:5555`), touch & dump drivers verified |
| **Package Separation Feasibility** | `applicationIdSuffix = ".verification"` on a dedicated build variant | **PASS** | Feasible; creates side-by-side app sandbox without altering production records |

---

## Stage 1 — Read-Only Technical Preflight Findings

### 1. Repository State & Git Status
- **Current Branch:** `fix/mutual-transfer-test-polling-regressions`
- **Current Commit (HEAD):** `cb1d47b32cef8e0ab20d5054f76c1261321d6879`
- **Working Tree:** Preserved with existing tracked modifications (54 files) and untracked artifacts/test reports from Batches 1–3. Zero git resets, stashes, or checkouts performed.

### 2. Application Build Variants & Application IDs
- **Namespace:** `com.pasindu.nursingotapp`
- **Default Application ID:** `com.pasindu.nursingotapp`
- **Compile SDK / Target SDK:** `36` / `36`, `minSdk = 26`
- **Current Build Types:** `release` (minified/shrinkResources) and default implicit `debug`.

### 3. Room Database Configuration & Creation Strategy
- **Database Class:** [`AppDatabase.kt`](file:///c:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/data/local/AppDatabase.kt) (Current Schema Version: `17`)
- **Instantiation:** Defined in [`DatabaseModule.kt`](file:///c:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/di/DatabaseModule.kt#L24-L30):
  ```kotlin
  val database = Room.databaseBuilder(
      context,
      AppDatabase::class.java,
      "nursing_ot_app.db"
  ).addMigrations(*DatabaseMigrationRegistry.ALL_MIGRATIONS).build()
  ```
- **File System Location:** Android resolves the path as `context.getDatabasePath("nursing_ot_app.db")`, located at:
  `/data/data/<applicationId>/databases/nursing_ot_app.db`
- **Automatic Seeders:** `SalaryTableSeeder.startSeeding(database)` and `KnowledgeTableSeeder.startSeeding(database, context)` trigger on database initialization.

### 4. ISBAR Entity, DAO, Use Cases & UI
- **Entity:** [`IsbarNoteEntity.kt`](file:///c:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/data/local/entity/IsbarNoteEntity.kt):
  - Fields: `id: Int`, `patientIdentifier: String`, `identification: String`, `situation: String`, `background: String`, `assessment: String`, `recommendation: String`, `timestamp: Long`.
- **DAO:** [`ClinicalPlanningDao.kt`](file:///c:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/data/local/dao/ClinicalPlanningDao.kt):
  - Methods: `insertIsbarNote`, `updateIsbarNote`, `deleteIsbarNote`, `getAllIsbarNotes(): Flow<List<IsbarNoteEntity>>`, `deleteOldNotes(cutoffTime: Long)`.
- **Use Cases:** [`ClinicalPlanningUseCases.kt`](file:///c:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/domain/usecase/ClinicalPlanningUseCases.kt):
  - `ObserveIsbarNotesUseCase`, `AddIsbarNoteUseCase`, `UpdateIsbarNoteUseCase`, `DeleteIsbarNoteUseCase`, `PurgeOldIsbarNotesUseCase`.
- **ViewModel:** [`ClinicalPlanningViewModel.kt`](file:///c:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/ui/ClinicalPlanningViewModel.kt):
  - Exposes `isbarNotes: StateFlow<List<IsbarNoteEntity>>`.
  - Methods: `addIsbarNote()`, `updateIsbarNote()`, `deleteIsbarNote()`, `purgeOldIsbarNotes()`.
- **UI & Confirmation Dialogs:** [`ClinicalPlanningDashboardScreen.kt`](file:///c:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/ui/screens/ClinicalPlanningDashboardScreen.kt):
  - Floating Action Button: "Add Note" opens `AlertDialog(title = "Add ISBAR Note")`.
  - Row Actions:
    - Edit: `IconButton` with `contentDescription = "Edit ISBAR note for ${note.patientIdentifier}"` opens `AlertDialog(title = "Edit ISBAR Note")`.
    - Delete: `IconButton` with `contentDescription = "Delete ISBAR note for ${note.patientIdentifier}"` triggers confirmation:  
      `AlertDialog(title = "Delete ISBAR Note?", text = "Are you sure you want to delete the ISBAR handover note for patient \"...\"? This clinical record will be permanently deleted.")` with buttons `[Cancel]` and `[Delete]`.
  - 48h Purge Action: `IconButton` in TopAppBar with `Icons.Default.DeleteSweep` and `contentDescription = "Purge >48h Notes"`.

### 5. Clinical Task Entity, DAO, Use Cases & UI
- **Entity:** [`ClinicalTaskEntity.kt`](file:///c:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/data/local/entity/ClinicalTaskEntity.kt):
  - Fields: `id: Int`, `taskName: String`, `description: String`, `priority: String` (`HIGH`, `MEDIUM`, `LOW`), `triggerTime: Long`, `isCompleted: Boolean = false`, `bypassDnd: Boolean = false`.
- **DAO:** [`ClinicalPlanningDao.kt`](file:///c:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/data/local/dao/ClinicalPlanningDao.kt):
  - Methods: `insertTask`, `updateTask`, `deleteTask`, `getAllTasks(): Flow<List<ClinicalTaskEntity>>`, `setTaskCompleted(taskId, completed)`.
- **Use Cases:** [`ClinicalPlanningUseCases.kt`](file:///c:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/domain/usecase/ClinicalPlanningUseCases.kt):
  - `ObserveClinicalTasksUseCase`, `AddClinicalTaskUseCase`, `UpdateClinicalTaskUseCase`, `DeleteClinicalTaskUseCase`, `SetClinicalTaskCompletedUseCase`.
- **UI & Confirmation Dialogs:** [`ClinicalPlanningDashboardScreen.kt`](file:///c:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/ui/screens/ClinicalPlanningDashboardScreen.kt):
  - Checkbox: Toggles completion state via `setTaskCompleted(taskId, !isCompleted)`.
  - Edit: `IconButton` with `contentDescription = "Edit task ${task.taskName}"` opens `AlertDialog(title = "Edit Clinical Task")`.
  - Delete: `IconButton` with `contentDescription = "Delete task ${task.taskName}"` triggers confirmation:  
    `AlertDialog(title = "Delete Clinical Task?", text = "Are you sure you want to delete \"${task.taskName}\"? This task will be permanently removed from your active shift plan.")` with `[Cancel]` and `[Delete]`.

### 6. PDF Generation Pipeline & File Handling
- **Engine:** [`PdfGenerator.kt`](file:///c:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/ui/otforms/PdfGenerator.kt)
  - Layout: Exact Ministry of Health two-page format (`drawForm1` on page 1, `drawForm2` on page 2).
  - Output File: `val outputDir = File(context.cacheDir, "pdf").apply { mkdirs() }`  
    `val outputFile = File(outputDir, "OT_Form_$majorityMonth.pdf")`
- **Preview & Validation UI:** [`PdfPreviewDialog.kt`](file:///c:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/ui/screens/PdfPreviewDialog.kt)
  - Loads pages into Bitmaps using Android standard `android.graphics.pdf.PdfRenderer`.
  - Displays header: "PDF Review — Review your OT claim before saving", page count indicator ("2 pages"), and action button "Save PDF".
- **Download & Share:** [`AppNavigation.kt`](file:///c:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/ui/navigation/AppNavigation.kt#L672-L678) saves to Android Downloads via MediaStore `FileShareUtils.savePdfToDownloads` and broadcasts an Android share intent `Intent.ACTION_SEND` (`application/pdf`).

### 7. Physical Device Automation Infrastructure
- **Connected Device:** Redmi Note 14 5G (`24094RAD4G`)
- **ADB Endpoint:** `10.150.55.191:5555`
- **Automation Driver:** `scratch/ui_automator_client.py` communicating with UI Automator 2.0 instrumentation server (`http://127.0.0.1:6790/jsonrpc`).
- **Capabilities Verified in Batch 3:**
  - Fast hierarchy dumping (`/dumpWindowHierarchy`)
  - Semantic and clickable ancestor resolution
  - Vertical/horizontal scrolling
  - Screenshot capture
  - Text input and element clicks without coordinate guessing or security bypasses

---

## Stage 2 — Proposed Isolated Test Environment

### The Core Problem
On the physical device, the user currently has an active installation of `com.pasindu.nursingotapp` with live duty records, real clinical handovers, and system configurations. Any write operation against `com.pasindu.nursingotapp` risks corrupting or deleting user data.

### Recommended Solution: Dedicated Verification Variant (`com.pasindu.nursingotapp.verification`)

By introducing a dedicated Gradle build type `verification` in [`app/build.gradle.kts`](file:///c:/Users/Shaan/StudioProjects/NursingOTApp/app/build.gradle.kts):

```kotlin
android {
    ...
    buildTypes {
        create("verification") {
            initWith(buildTypes.getByName("debug"))
            applicationIdSuffix = ".verification"
            versionNameSuffix = "-verification"
            matchingFallbacks += listOf("debug")
        }
    }
}
```

#### Why This Is 100% Non-Destructive:
1. **Linux Sandbox Isolation:** Android treats `com.pasindu.nursingotapp.verification` as an entirely distinct application from `com.pasindu.nursingotapp`.
2. **Independent Storage:**
   - Production Data: `/data/data/com.pasindu.nursingotapp/` (**UNT当下TOUCHED**)
   - Verification Data: `/data/data/com.pasindu.nursingotapp.verification/` (**ISOLATED TEST SANDBOX**)
3. **Independent Room Database:**
   - Production DB: `/data/data/com.pasindu.nursingotapp/databases/nursing_ot_app.db`
   - Verification DB: `/data/data/com.pasindu.nursingotapp.verification/databases/nursing_ot_app.db`
4. **Independent Preferences & Cache:**
   - SharedPreferences, cache files, and generated PDF files live strictly within the `.verification` directory.
5. **Side-by-Side Coexistence:**
   - Both the user's real app and the verification app can be installed simultaneously on the Redmi Note 14 5G.
   - The production app is never uninstalled, updated in place, or cleared.
6. **Zero Mock Code:**
   - The verification variant compiles the exact same Kotlin source code, Jetpack Compose screens, ViewModels, Room DAOs, and PDF generator as production.

#### Handling Google Services Plugin Compatibility
In `app/google-services.json`, the only registered package name is `com.pasindu.nursingotapp`. When `applicationIdSuffix = ".verification"` is added, the Gradle Google Services task (`processVerificationGoogleServices`) looks for `com.pasindu.nursingotapp.verification`.

To maintain seamless compilation without touching production code or requiring Firebase modifications:
- Add a client entry in `google-services.json` matching `com.pasindu.nursingotapp.verification`, OR
- Disable `processVerificationGoogleServices` in Gradle configuration via:
  ```kotlin
  project.afterEvaluate {
      tasks.matching { it.name.contains("GoogleServices") && it.name.contains("Verification") }.configureEach {
          enabled = false
      }
  }
  ```
  *(Note: Firebase is strictly used for Mutual Transfer; core Clinical CRUD and PDF generation have zero Firebase dependency).*

---

## Stage 3 — Exact Automated Test Sequence

### Test Case B4-01: ISSUE-07 — ISBAR Note CRUD & 48h Purge Verification
- **App Under Test:** `com.pasindu.nursingotapp.verification`
- **Execution Script:** `scratch/verify_batch4_clinical_isbar.py`
- **Steps:**
  1. Launch `com.pasindu.nursingotapp.verification`.
  2. Navigate to `More` -> `Clinical Planning & ISBAR` -> Tab `ISBAR Notes`.
  3. Verify empty initial state (or only seed data).
  4. Tap `Add Note` (Floating Action Button).
  5. Enter synthetic patient note:
     - Patient Identifier: `TEST-PT-901`
     - Identification: `Bed 14 - Post-Op Laparotomy`
     - Situation: `Pain score 4/10, vitals stable, IV fluids running.`
     - Background: `Admitted Day 2 post-op, no known drug allergies.`
     - Assessment: `Abdomen soft, dressing clean and dry, urine output adequate.`
     - Recommendation: `Maintain fluid balance chart, administer analgesia as charted.`
  6. Tap `Save Note`.
  7. **Verify Creation:** Check UI hierarchy for `TEST-PT-901` and verify presence. Capture screenshot: `B4_ISBAR_01_created.png`.
  8. Tap Edit button (`Edit ISBAR note for TEST-PT-901`).
  9. Change Situation to: `Pain score reduced to 2/10 following analgesia, patient resting comfortably.`
  10. Tap `Save Note`.
  11. **Verify Update:** Verify updated text appears on card. Capture screenshot: `B4_ISBAR_02_updated.png`.
  12. Reopen Edit dialog and confirm persistence of all fields. Tap `Cancel` or dismiss.
  13. Tap Delete button (`Delete ISBAR note for TEST-PT-901`).
  14. **Verify Delete Prompt:** Check dialog text: `"Are you sure you want to delete the ISBAR handover note for patient "TEST-PT-901"?"`. Capture screenshot: `B4_ISBAR_03_delete_dialog.png`.
  15. Tap `Cancel`.
  16. **Verify Cancellation:** Confirm `TEST-PT-901` remains in list.
  17. Tap Delete button again, then tap `Delete` (Confirm).
  18. **Verify Deletion:** Confirm `TEST-PT-901` is removed from hierarchy. Capture screenshot: `B4_ISBAR_04_deleted.png`.
  19. **Verify 48h Purge Trigger:** Tap `Purge >48h Notes` (TopAppBar `DeleteSweep`). Verify action runs without exceptions. Capture screenshot: `B4_ISBAR_05_purged.png`.

### Test Case B4-02: ISSUE-08 — Clinical Task CRUD & Completion Toggle Verification
- **App Under Test:** `com.pasindu.nursingotapp.verification`
- **Execution Script:** `scratch/verify_batch4_clinical_tasks.py`
- **Steps:**
  1. Navigate to `Clinical Planning & ISBAR` -> Tab `Shift Tasks`.
  2. Tap `Add Task` (Floating Action Button).
  3. Enter synthetic task:
     - Task Name: `Administer IV Ceftriaxone 1g`
     - Description: `Reconstitute in 10ml sterile water, infuse over 15 mins.`
     - Priority: `HIGH`
     - Bypass DND: `Checked`
  4. Tap `Save Task`.
  5. **Verify Creation:** Check UI hierarchy for task name, priority badge (`HIGH`), and bypass DND indicator. Capture screenshot: `B4_TASK_01_created.png`.
  6. Tap Task Checkbox to toggle completion.
  7. **Verify Completion:** Confirm task is marked complete (strikethrough / completed state). Capture screenshot: `B4_TASK_02_completed.png`.
  8. Tap Edit button (`Edit task Administer IV Ceftriaxone 1g`).
  9. Change Task Name to: `Administer IV Ceftriaxone 1g + Gentamicin 80mg`.
  10. Change Priority to: `MEDIUM`.
  11. Tap `Save Task`.
  12. **Verify Persistence & Preservation:** Confirm that `isCompleted` remains `true` and `bypassDnd` remains preserved after title/priority edit. Capture screenshot: `B4_TASK_03_edited.png`.
  13. Tap Delete button. Verify confirmation dialog: `"Are you sure you want to delete "Administer IV Ceftriaxone 1g + Gentamicin 80mg"?"`.
  14. Tap `Cancel`. Confirm task still exists.
  15. Tap Delete button, tap `Delete` (Confirm).
  16. **Verify Deletion:** Confirm task disappears from UI hierarchy. Capture screenshot: `B4_TASK_04_deleted.png`.

### Test Case B4-03: ISSUE-13 — Synthetic Profile, Calendar & PDF File Verification
- **App Under Test:** `com.pasindu.nursingotapp.verification`
- **Execution Script:** `scratch/verify_batch4_pdf_generation.py`
- **Steps:**
  1. Navigate to `Profile` screen.
  2. Populate synthetic profile:
     - Full Name: `S. P. K. Jayasinghe`
     - Service No: `N-45892`
     - Unit: `Ward 12B - Acute Medical`
     - Pay Sheet No: `PS-88741`
     - Grade: `Grade I`
     - Basic Salary: `45,000.00`
     - OT Rate: `320.00`
  3. Save profile and verify persistence.
  4. Navigate to `Work` -> `Normal Planner` -> Synthetic Month (`2026-11-01` to `2026-11-28`).
  5. Apply `Standard Ward (Day/Full)` preset with `Preserve Leaves & Off Days`.
  6. Tap `Preview PDF` before saving.
  7. **Verify Unsaved Edits Blocker:** Confirm dialog `"Unsaved Changes — You have 27 unsaved calendar edit(s)"`. Capture screenshot: `B4_PDF_01_unsaved_blocked.png`.
  8. Dismiss dialog, tap `Save selected days` to commit the synthetic calendar into the verification Room database.
  9. Tap `Preview PDF`.
  10. **Verify Generation & Dialog:** App generates PDF and presents `PdfPreviewDialog`.
      - Dialog Title: `PDF Review`
      - Page Count: `2 pages`
      - Badge: `READY` / `Ready to save`
      - Capture screenshot: `B4_PDF_02_preview_dialog.png`.
  11. **Direct File Inspection via ADB:**
      - Pull generated file from `/data/data/com.pasindu.nursingotapp.verification/cache/pdf/OT_Form_November.pdf` to `audit-output/physical-device/artifacts/OT_Form_November.pdf`.
      - Inspect file size (must be > 20 KB).
      - Run Python PDF analysis on host to verify:
        - Exact page count = `2`
        - MediaBox dimensions = `595 × 842 pt` (Standard A4)
        - Profile strings embedded (`S. P. K. Jayasinghe`, `N-45892`, `Ward 12B - Acute Medical`, `Grade I`, `45000`, `320`)
        - Duty hours and OT hours match deterministic engine output (`168.0` normal hours, `24.0` OT hours).
      - Capture rendered page 1 and page 2 image artifacts.
  12. Tap `Save PDF` in the dialog to verify save action triggers smoothly. Capture screenshot: `B4_PDF_03_saved.png`.

---

## Potential Risks & Mitigation Strategy

| Risk | Severity | Mitigation Strategy |
| :--- | :---: | :--- |
| **Accidental Overwrite of Production App** | Critical | Use `applicationIdSuffix = ".verification"` so the package name is strictly `com.pasindu.nursingotapp.verification`. ADB install commands target only this package. |
| **Accidental Modification of User Data** | Critical | The verification app has its own isolated Linux UID and its own database path `/data/data/com.pasindu.nursingotapp.verification/databases/`. Android OS enforces strict sandbox boundaries between distinct package IDs. |
| **Google Services Package Mismatch** | Moderate | Handle via Gradle task configuration (`processVerificationGoogleServices.enabled = false`) or a matching verification client entry in `google-services.json`. |
| **Display Timeout / Device Sleep** | Low | Driver automatically keeps device awake via `svc power stayon true` and `settings put system screen_off_timeout 1800000`. |
| **Touch Coordinate Inaccuracies** | Low | Driver uses Compose semantic text and content description selectors with dynamic clickable ancestor resolution rather than fixed pixel coordinates. |

---

## Required Gradle Build & Test Commands

1. **Build Verification Variant APK:**
   ```powershell
   .\gradlew.bat :app:assembleVerification
   ```
2. **Install Verification Variant to Physical Device (Side-by-Side):**
   ```powershell
   adb -s 10.150.55.191:5555 install -r app/build/outputs/apk/verification/app-verification.apk
   ```
3. **Verify Side-by-Side Package Registration:**
   ```powershell
   adb -s 10.150.55.191:5555 shell "pm list packages | grep nursingotapp"
   # Output must show BOTH:
   # package:com.pasindu.nursingotapp
   # package:com.pasindu.nursingotapp.verification
   ```
4. **Execute Automated Verification Suite:**
   ```powershell
   python scratch/verify_batch4_clinical_isbar.py
   python scratch/verify_batch4_clinical_tasks.py
   python scratch/verify_batch4_pdf_generation.py
   ```
5. **Clean Up Verification Package Post-Verification (Optional & Non-Destructive):**
   ```powershell
   adb -s 10.150.55.191:5555 uninstall com.pasindu.nursingotapp.verification
   ```
   *(Uninstalls only the verification package; leaves `com.pasindu.nursingotapp` completely untouched).*

---

## Conclusion & Next Steps

All technical prerequisites for **Batch 4** have been audited and verified. The proposed **verification application variant (`com.pasindu.nursingotapp.verification`)** provides guaranteed isolation, side-by-side coexistence, and zero risk to real user records, while testing the exact production UI and business logic on the physical Redmi Note 14 5G.

**Preflight Status:** **READY FOR AUTHORIZATION**  
*Awaiting explicit user approval before configuring the build variant or running automated physical tests.*
