# NURSING SUPER APP — BATCH 3 READ-ONLY PREFLIGHT REPORT
## Daily Entry / Normal Planner: Configurable Duty Presets & Accessible PDF Export Center

**Application:** NursingOTApp (`com.pasindu.nursingotapp`)  
**Workspace:** `C:\Users\Shaan\StudioProjects\NursingOTApp`  
**Git Branch:** `fix/mutual-transfer-test-polling-regressions`  
**Git HEAD:** `cb1d47b32cef8e0ab20d5054f76c1261321d6879`  
**Review Mode:** READ-ONLY PREFLIGHT INSPECTION  
**Date:** 2026-11-06  
**Target Remediation Issues:**
- `ISSUE-12`: Daily Entry / Normal Planner: Configurable duty presets (ward-specific shift patterns).
- `ISSUE-13`: Daily Entry / Planner PDF: Improve PDF export accessibility and pre-export validation.

---

## 1. Executive Summary & Audit Context

During the comprehensive real-device UI and functional audit, two significant defects were identified in the primary workflow of the application (`DailyEntryScreen.kt`):

1. **ISSUE-12 (Functional — P2):** The duty auto-fill engine forces a single, rigid duty pattern (`07:00–16:00` on weekdays, `07:00–13:00` on weekends) without allowing the nurse to select their ward or unit shift pattern (e.g. Standard Ward 6h/12h rotation, ICU 12h rotation, or Clinic/Day duties). Furthermore, the Smart Auto-Fill feature is artificially restricted to `Special` wards in the UI, leaving `Normal` ward nurses with a rudimentary brush shortcut.
2. **ISSUE-13 (Export & Accessibility — P2):** The "Generate PDF" action is a small, unprominent icon button buried in the TopAppBar (`DailyEntryScreen.kt:281`), lacking pre-export verification. Nurses can trigger PDF compilation while holding unsaved calendar edits, unmatched salary steps, or incomplete rosters with no visual checklist, progress clarity, or actionable resolution guidance.

This preflight report evaluates the exact codebase, entities, calculation engines, and PDF generation pipelines to formulate a surgical, 100% additive remediation architecture that protects all financial calculations, avoids Room migrations, and preserves the official Ministry PDF format.

---

## 2. Git Working-Tree & Safety Status

- **Current Branch:** `fix/mutual-transfer-test-polling-regressions`
- **Current HEAD Commit:** `cb1d47b32cef8e0ab20d5054f76c1261321d6879` (*Checkpoint Phase 5 progress: backup, FCM foundation and app regression fixes*)
- **Working-Tree Compliance:**
  - All pre-existing modified files from earlier phases (Phase 13, Batch 1, Batch 2) were preserved intact.
  - Zero Git mutations performed (`no commit`, `no checkout`, `no reset`, `no stash`, `no clean`, `no branch switch`).
  - No database resets, Room alterations, or app-data clearings performed.
  - Zero application code modified during this preflight turn.

---

## 3. Exact Source Files Inspected

| Category | File Path | Key Inspected Areas |
|---|---|---|
| **Daily Entry UI** | [`app/src/main/java/.../ui/screens/DailyEntryScreen.kt`](file:///C:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/ui/screens/DailyEntryScreen.kt) | Auto-fill logic (lines 418–435, 551–585), Calendar grid (lines 670–895), Bottom bar summary (lines 285–490), PDF export trigger (lines 255–281). |
| **PDF Generation** | [`app/src/main/java/.../ui/otforms/PdfGenerator.kt`](file:///C:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/ui/otforms/PdfGenerator.kt) | Form 1 & Form 2 bitmap drawing, coordinate mapping scaling (`sX`, `sY`), Sinhala font integration, cache output. |
| **PDF Preview** | [`app/src/main/java/.../ui/screens/PdfPreviewDialog.kt`](file:///C:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/ui/screens/PdfPreviewDialog.kt) | `PdfRenderer` page rendering, zoom/pan controls, confirm/share flow. |
| **Navigation & Wiring** | [`app/src/main/java/.../ui/navigation/AppNavigation.kt`](file:///C:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/ui/navigation/AppNavigation.kt) | `generateOtPdf()` helper, `WeeklyOtCalculator.calculate()`, rate resolution. |
| **ViewModel** | [`app/src/main/java/.../ui/NursingViewModel.kt`](file:///C:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/ui/NursingViewModel.kt) | `dailyLogs`, `saveDailyEntry`, `deleteDailyEntry`, `calculateSavedDailyEntryHours`. |
| **Domain Engine** | [`app/src/main/java/.../domain/calculation/WeeklyCalculationEngine.kt`](file:///C:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/domain/calculation/WeeklyCalculationEngine.kt) | `calculateShiftHours`, `calculateMaxConsecutiveNightShifts`, overnight shifts. |
| **Domain OT Calc** | [`app/src/main/java/.../domain/ot/WeeklyOtCalculator.kt`](file:///C:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/domain/ot/WeeklyOtCalculator.kt) | Full-week 36h threshold, PH days, DO days, OT amounts. |
| **Data Entities & DAOs**| [`app/src/main/java/.../data/local/entity/DailyEntryEntity.kt`](file:///C:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/data/local/entity/DailyEntryEntity.kt)<br>[`app/src/main/java/.../data/local/dao/DailyEntryDao.kt`](file:///C:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/data/local/dao/DailyEntryDao.kt) | Persistence structure: `normalTimeIn`, `normalTimeOut`, `normalHours`, `otTimeIn`, `otTimeOut`, `otHours`. |
| **Design System** | [`app/src/main/java/.../ui/theme/Color.kt`](file:///C:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/ui/theme/Color.kt)<br>[`app/src/main/java/.../ui/theme/Dimensions.kt`](file:///C:/Users/Shaan/StudioProjects/NursingOTApp/app/src/main/java/com/pasindu/nursingotapp/ui/theme/Dimensions.kt) | Semantic palettes, 48dp touch bounds, surface colors. |

---

## 4. ISSUE-12: Configurable Duty Presets Analysis

### 4.1 Current Behaviour & Limitations
1. **Hardcoded Pattern:**
   In `DailyEntryScreen.kt:424`, auto-fill handles unassigned days with:
   ```kotlin
   if (isAutoFillMode && nIn.isEmpty() && edit.leave == null && !isL) {
       nIn = "07.00"; nOut = if (isShortDay) "13.00" else "16.00"; nHrs = if (isShortDay) 6f else 9f; isL = false; lType = null
   }
   ```
   This assumes all nurses work Day duty (`07.00–16.00`, 9h) on weekdays and Morning duty (`07.00–13.00`, 6h) on weekends.
2. **Ward Type Inequity:**
   - Normal Ward: Shows only "Apply to remaining days" card (lines 551–570), which forces `selectedBrush = "Morn (7-13)"`.
   - Special Ward: Shows "Smart Auto-Fill" dialog (lines 573–585), but confirms without letting the nurse choose their shift pattern.
3. **No ICU / Night-Heavy Roster Support:**
   In Sri Lankan hospitals, ICU and Emergency units frequently operate on 12-hour rotations (`07:00–19:00` Day, `19:00–07:00` Night), while standard wards rotate Morning (`07:00–13:00`), Evening (`13:00–19:00`), and Night (`19:00–07:00`). Nurses currently must manually paint dozens of individual days.

### 4.2 Storage & Time Format Facts
- **Storage:** Plain strings in `DailyEntryEntity`: `normalTimeIn = "07.00"`, `normalTimeOut = "13.00"`, `normalHours = 6.0f`.
- **Overnight Shifts:** Handled seamlessly by setting `normalTimeIn = "19.00"`, `normalTimeOut = "07.00"`, `normalHours = 12.0f`. `WeeklyCalculationEngine` detects `end < start` and correctly aggregates 12.0 hours.
- **Breaks:** In the Sri Lankan public health service, nurse shifts are scheduled in standard continuous blocks; no break deductions exist in the official claim formulas.
- **Incomplete / Invalid Entries:** Blank strings `""` default safely to `0f` hours without exceptions.

### 4.3 Proposed Minimal Additive Solution for ISSUE-12
1. **Configurable Preset Model (UI / Domain State):**
   Define clear, standard clinical duty presets:
   - **Preset 1: Standard Ward (Morning Focus)**
     - Weekday: Morning (`07.00–13.00`, 6h)
     - Weekend / Short Day: Morning (`07.00–13.00`, 6h)
   - **Preset 2: Standard Ward (Day / Full Duty Focus)**
     - Weekday: Day (`07.00–16.00`, 9h)
     - Weekend / Short Day: Morning (`07.00–13.00`, 6h)
   - **Preset 3: ICU / Emergency (12h Shift Rotation)**
     - Weekday / Weekend: Day 12h (`07.00–19.00`, 12h) or Night 12h (`19.00–07.00`, 12h)
   - **Preset 4: Standard Ward (Evening Focus)**
     - Weekday / Weekend: Evening (`13.00–19.00`, 6h)
   - **Preset 5: Night Shift Heavy Rotation**
     - Night (`19.00–07.00`, 12h)
2. **Unified Auto-Fill Preset Selector Dialog:**
   - Available to **BOTH** Normal Ward and Special Ward users.
   - Allows nurses to:
     1. Choose their active duty preset pattern.
     2. Toggle "Preserve existing Leaves, DOs & PHs" (defaults to true).
     3. Preview the total duty hours before applying.
3. **Zero Entity / Migration Impact:**
   - Presets populate the existing `stagedEdits` map with standard valid string values (`"07.00"`, `"13.00"`, etc.).
   - Database schema, DAOs, and Room tables remain 100% unchanged.

---

## 5. ISSUE-13: PDF Export Accessibility & Pre-Export Validation Analysis

### 5.1 Current Behaviour & Deficiencies
1. **Low Visibility Action:**
   The only export trigger is `IconButton(onClick = { ... }) { Icon(Icons.Default.PictureAsPdf, ...) }` in the TopAppBar (line 281). There is no prominent CTA in the screen's main scrolling content or bottom bar.
2. **Zero Pre-Export Validation:**
   When tapped, it immediately calls `generateOtPdf()` with whatever state is in the database:
   - **Unsaved Edits Trap:** If the nurse made staged edits in the calendar but didn't tap "Save", the PDF generates using obsolete database records!
   - **Salary Step Failure:** If the user's basic salary or grade is unmatched in the 2027 salary scale, generation aborts with a brief, easily missed snackbar.
   - **Incomplete Month Warning:** If 10 days of the month are unlogged, the PDF compiles with blank rows without asking the nurse if they intended to export an incomplete claim.
3. **Accessibility:**
   The TopAppBar icon is small (24dp icon inside standard bounds) and provides no context on claim readiness.

### 5.2 Critical Preservation Constraint
> **ABSOLUTE RULE:** The generated PDF layout, coordinates (`OT_Form_Cell_Coordinates.md`), background templates (`form_front_bg`, `form_back_bg`), Sinhala typeface rendering, and math in `WeeklyOtCalculator` **MUST NOT BE CHANGED**. The enhancement is strictly focused on **pre-export validation, UX flow, and export accessibility**.

### 5.3 Proposed Minimal Additive Solution for ISSUE-13
1. **Pre-Export Validation Engine (`ClaimPreExportValidator`):**
   Evaluate 4 unambiguous clinical and financial readiness checks:
   - **Check 1: Profile & Service Data** (Name, Service No, Grade, Ward present).
   - **Check 2: Pay Scale & OT Rate Resolution** (`PayRateResolver.resolve(profile)` matches grade and step; OT rate is valid).
   - **Check 3: Calendar Completeness** (Count of logged days vs total calendar days; warns if days remain unassigned).
   - **Check 4: Unsaved Staged Edits** (Warns if `stagedEdits.isNotEmpty()`, prompting nurse to save first so edits appear in the PDF).
2. **Dedicated Export Center Card / Bottom Sheet:**
   - Add a high-visibility, accessible **"Export Center" Card** directly above the calendar or accessible via a prominent bottom action.
   - Displays a clean readiness scorecard:
     -  Profile & Rates Verified (`Rs. X / hour`)
     -  Unsaved Changes Alert (if any staged edits remain)
     -  Calendar Status (`31/31 days recorded` or `24/31 days recorded — 7 unlogged`)
     -  Calculated Claim Summary (`XX.X OT Hours • Rs. XX,XXX estimated`)
   - Prominent 48dp Primary Button: **"Review & Generate PDF Form"**.
3. **Actionable Error & Progress Handling:**
   - If salary is unmatched, present a clear dialog with a direct shortcut: *"Your basic salary does not match the 2027 nursing grade scale. Go to Profile to update salary."*
   - During generation, show an accessible progress dialog with clear status text.
   - Retain the existing `PdfPreviewDialog` for visual inspection and Android sharing via `FileShareUtils`.

---

## 6. Existing Business Rules That Must Remain Unchanged

1. **Weekly OT Calculation:** The full-week Sunday-to-Saturday 36-hour ceiling rule in `WeeklyOtCalculator` must not be altered.
2. **Financial Math:** Rates for standard OT (1.5x / step rate), Public Holidays, and Days Off must not be modified.
3. **PDF Template & Formatting:** Cell centers, font sizes, text alignments, and Sinhala text drawing in `PdfGenerator.kt` must remain 100% identical.
4. **48-Hour ISBAR Retention:** Unrelated clinical tools retain their exact existing retention and manual purge behavior.
5. **Existing Brush Semantics:** All existing brushes (`Morn (7-13)`, `Eve (13-19)`, `Night (19-7)`, `Day (7-16)`, `CL`, `VL`, `Work DO`, etc.) must remain functional.

---

## 7. Design Research & UI Pattern Grounding

### 7.1 Visual System Compatibility
- **Primary Color:** `MedicalBlue` (`#0EA5E9`) and `DailyCyan` (`#0284C7`).
- **Success Tone:** `Emerald` (`#10B981`) and `DailyMintSoft` (`#DDF4EA`).
- **Warning Tone:** `Amber` (`#F59E0B`) and `DailyAmberSoft` (`#FFF7ED`).
- **Touch Target:** Strict enforcement of `NursingDimensions.TouchTarget.minimum` (`48.dp`).
- **Card Surfaces:** `Color.White` with 20.dp rounded corners and subtle shadow (`elevation = 2.dp`).

### 7.2 Clinical & Document UX References
- **Shift Rostering Patterns (NHS Health Roster / Deputy / When I Work):**
  - Shift workers operate in predictable shift blocks. Preset auto-filling allows nurses to apply recurring rotations while respecting exceptions (leaves, days off) already on the board.
- **Pre-Flight Compliance Export (Intuit / TurboTax / DocuSign):**
  - High-stakes financial and legal compliance documents benefit greatly from a pre-flight readiness checklist. Showing users whether all prerequisites are met prevents invalid claims from reaching hospital pay offices.

---

## 8. Room Database & Data-Integrity Risks

| Risk Area | Risk Level | Mitigation Strategy |
|---|---|---|
| **Room Database Migrations** | **ZERO RISK** | No database tables or columns are added or modified. Presets and pre-export validation operate entirely in memory within the UI/ViewModel layer. Database schema remains at v17. |
| **Accidental Data Overwrite** | **LOW** | Auto-fill preset engine explicitly checks `edit.leave == null && !existing.isLeave && !existing.isDO && !existing.isPH` before applying duty presets, ensuring planned leaves and holidays are never overwritten. |
| **Unsaved Staged Edits Lost on PDF Export** | **ELIMINATED** | Pre-export validation flags unsaved edits with a clear "Save edits before generating" prompt. |

---

## 9. Testing Strategy for Batch 3

1. **Unit Tests (JVM):**
   - Test preset generation logic (verifies that selecting Standard Ward, ICU 12h, or Night presets generates exact expected start/end times and hours).
   - Test pre-export validation rules (profile missing, salary unmatched, unsaved edits, incomplete days, fully valid state).
2. **UI State & Accessibility Tests:**
   - Verify 48dp touch targets on preset selector chips and Export Center buttons.
   - Verify accessible content descriptions on all new interactive components.
3. **Regression Tests:**
   - Run the full suite of 424 existing unit tests (`.\gradlew testDebugUnitTest`) to guarantee 0 regressions across calculation engines and ViewModels.
   - Run `.\gradlew assembleDebug` to verify compilation.

---

## 10. Blockers, Uncertainties & Open Decisions

- **Blockers:** **NONE.** All required data sources (`dailyLogs`, `userProfile`, `payRateResolver`, `WeeklyOtCalculator`) are already accessible in `NursingViewModel` and `AppNavigation`.
- **Uncertainties:** None. Presets and pre-export checklist will be cleanly integrated into `DailyEntryScreen.kt` without changing any existing calculation or database files.

---

## 11. Final Recommendation

**RECOMMENDATION:** **PROCEED TO IMPLEMENTATION AUTHORIZATION**

The proposed solutions for ISSUE-12 and ISSUE-13 are 100% additive, technically robust, and fully protect the underlying database schemas, financial calculations, and PDF drawing code. 

**Next Step:** Await explicit user authorization before making any code modifications.
