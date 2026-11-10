# DESIGN & ACCESSIBILITY RESEARCH — BATCH 1 UI IMPLEMENTATION

**Project:** NursingOTApp (`com.pasindu.nursingotapp`)  
**Target:** Implementation Batch 1 (UI Consistency, Guide Consolidation, Touch Accessibility & Metric Alignment)  
**Date:** November 2026  
**Audited Baseline:** `UI_AUDIT_REPORT.md` (Physical device: Redmi Note 14 5G, Android 16 / HyperOS)

---

## 1. Material Design 3 Touch Target Standards (WCAG 2.5.5 / Android Accessibility)

### Guideline Reference
- **WCAG 2.1 Success Criterion 2.5.5 (Target Size):** The size of the target for pointer inputs is at least 44 by 44 CSS pixels (or 48dp on Android).
- **Google Android Accessibility Quality Guidelines:** Interactive UI controls MUST have a touch target of at least 48dp × 48dp (96px at 2.0x, 132px at 2.75x density).
- **Material 3 Token:** `LocalMinimumInteractiveComponentSize` defaults to 48.dp. When custom components use small icons or custom `Surface`/`Box` wrappers without standard sizing, the semantic bounding box shrinks below 48dp, causing accessibility service failures and physical touch misses.

### Applications to Batch 1 Issues
1. **Clinical Tool Cards (`ISSUE-05`):**
   - The "OPEN" action pill inside `RefinedToolCard` was styled as `Surface(color = tool.accent, shape = CircleShape) { Text("OPEN", Modifier.padding(11.dp, 8.dp), fontSize = 8.sp) }`. This gave a height of ~27dp.
   - Solution: Enforce `Modifier.defaultMinSize(minWidth = 56.dp, minHeight = 48.dp)` with centered alignment for the action pill.
   - The Back button in `ClinicalToolsRefinedScreen` was `Box(Modifier.size(44.dp))`.
   - Solution: Upgrade to `Modifier.size(48.dp)` or standard `IconButton(modifier = Modifier.size(48.dp))`.
2. **Home Profile Action (`ISSUE-15`):**
   - In `HomeWelcomeHeader`, the chevron icon had `contentDescription = "Open profile"` with `Modifier.size(27.dp)`, exposing a 27.6dp × 27.6dp bounds to accessibility services.
   - Solution: Wrap in `Box(Modifier.size(48.dp), contentAlignment = Alignment.Center)` with semantic button role and `onClick = onProfile`.
3. **Global Floating Action Button (`ISSUE-16`):**
   - In `NursingGuideOverlay.kt`, `NursingGuideFab` utilized `SmallFloatingActionButton`, which Material 3 defaults to 40dp × 40dp container size (measuring 41.1dp on 2.75x density).
   - Solution: Ensure `Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)` or standard `FloatingActionButton(modifier = Modifier.size(48.dp))` ensuring standard 48dp touch bounds.

---

## 2. Information Architecture & Single Entry Point Pattern (Guide Consolidation)

### Defect Context (`ISSUE-01`, `ISSUE-02`, `ISSUE-03`)
- In `AdvancedFinanceHubScreen`, users encountered THREE distinct guide triggers:
  1. Top app bar action chip: "Guide" -> opens `FinanceGuideDialog`.
  2. Full-width purple card: "Financial Planning" -> also opens `FinanceGuideDialog`, confusing users who expected a planning tool or calculator.
  3. Root navigation overlay: `NursingGuideFab` -> opens `NursingGuideDialog`.
- In `AnalyticsScreen` (Smart Insights):
  1. Top app bar Info action icon -> toggles inline `SmartInsightsGuideCard`.
  2. Root navigation overlay: `NursingGuideFab` -> opens modal `NursingGuideDialog`.

### Architectural Resolution
- **Rule:** Exactly ONE clear, predictable guide entry point per screen.
- For `advanced_finance_hub`:
  - Retain the top app bar action pill ("Guide"), opening the comprehensive `FinanceGuideDialog`.
  - Remove the misleading `FinancialPlanningBanner` card.
  - Suppress the global `NursingGuideFab` by removing `"advanced_finance_hub"` from `guideForRoute()`.
- For `analytics`:
  - Retain the top app bar info icon expanding the contextual `SmartInsightsGuideCard`.
  - Suppress the global `NursingGuideFab` by removing `"analytics"` from `guideForRoute()`.

---

## 3. Clinical Ergonomics & Whole-Card Interaction Pattern (`ISSUE-04`)

### User Need & Bedside Context
- In high-stress clinical environments (e.g. ICU, acute surgical wards), nurses frequently access calculation tools (IV drip rates, insulin infusions, BSA, weight-based titrations) using single-hand touch.
- Requiring a precise tap on a tiny 27dp "OPEN" pill while the rest of the large card only toggles a disclaimer text box creates severe cognitive and interaction friction.

### Interaction Redesign
- **Whole Card Clickable:** `Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen))` launches the calculation tool immediately.
- **Dedicated Disclaimer Toggle:** Provide an explicit, accessible info button (`IconButton` or 48dp target) specifically for inspecting verification engine disclaimers and safety guidance.
- **Primary Pill Visual:** Keep the "OPEN" pill as a clear visual affordance with ≥48dp touch target, also triggering `onOpen`.

---

## 4. Design System Consistency — Pay Sheet Bank (`ISSUE-09`)

### Current Inconsistencies
- `PaySheetBankScreen` used a raw custom `Surface { Row(...) }` instead of the standard Material 3 `TopAppBar` utilized across the rest of NursingOTApp.
- Used unstyled all-caps text `ADD FIRST PAY SHEET` in the empty state button, contrasting with the app's refined title/sentence-case typography.

### Standardized Layout
- Integrate standard `TopAppBar` with back navigation icon, title ("Pay Sheet Bank"), subtitle ("Your private monthly paysheet vault"), and "Private" badge chip.
- Standardize empty state button to `ClinicalPrimaryColor`, `RoundedCornerShape(16.dp)`, min-height 48dp, and title-cased text "Add First Pay Sheet".

---

## 5. Metric Row Visual Alignment (`ISSUE-10`, `ISSUE-11`)

### Alignment Defect Cause
- In `NurseCommandCenterModernScreenV2`, the `MetricRow` displays Duty (`h`), OT (`h`), and Net (`Rs.`). The `Net` card includes an additional conditional detail line (`financialHealthLabel(state)`). Without fixed heights or intrinsic measurement, the Net card renders significantly taller than Duty and OT.
- In `CarePulseModernScreen`, "Duty hours this claim period" wraps onto multiple lines while "Tasks" is a single word, causing the 3 metric summary cards to render with uneven heights.

### Layout Resolution
- Apply `Modifier.height(IntrinsicSize.Max)` to the parent `Row`.
- Apply `Modifier.weight(1f).fillMaxHeight()` to each child `Metric` / `Card`.
- Apply `fillMaxHeight()` and consistent vertical arrangement to the internal content `Column`.
- This guarantees uniform outer card bounding boxes across diverse device densities and font scale factors without clipping text.

---

## 6. Text Encoding & Typographic Polish (`ISSUE-14`)

### Root Cause
- Raw UTF-8 bullet characters (`•`) embedded directly in source code can suffer byte corruption depending on host file system, Git filter settings, or compiler character encoding options.
- The Shift snapshot subtitle in `HomeScreen` previously rendered a replacement glyph.

### Solution
- Standardize all bullet separators to the escaped unicode representation `\u2022`.
