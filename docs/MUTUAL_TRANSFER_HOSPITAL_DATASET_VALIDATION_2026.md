# Mutual Transfer Hospital Dataset — 2026 Workbook Validation

Source workbook: Sri_Lanka_Hospitals_List_2026.xlsx
Canonical source sheet: All Hospitals
Validation date: 2026-09-25

## Counts

Total workbook rows: 1,216

Included hospital-type rows: 1,206

Excluded rows:
- Arogya Center: 5
- Special Campaign / Other: 3
- Other Hospitals: 2

Excluded total: 10

## Included category counts

- NH: 3
- TH: 12
- Special TH: 5
- Special: 14
- Board Managed Hospital: 2
- DGH: 20
- BHA: 35
- BHB: 46
- DHA: 66
- DHB: 147
- DHC: 279
- PMCU: 577

Included total: 1,206

## Integrity checks

- Source row numbers are unique across the included records.
- Institution-name duplicates exist in the workbook and are retained because names alone are not treated as unique identifiers.
- No HIN values were invented.
- No coordinates were invented.
- Authority was not inferred where the workbook does not provide an explicit authority field.
- Functional status was not inferred where the workbook does not explicitly provide it.
- Workbook Remarks values are preserved.
- The five Arogya Centers are excluded from transfer destinations.

## Traceability

Each canonical record uses hospitalId = MOH2026-<source row number> and retains sourceRow.

The generated working artifact is:
Sri_Lanka_Hospitals_2026_Canonical_1206.csv

This dataset is not yet a Firestore seed. Coordinate/HIN enrichment and final authority/status verification remain separate controlled steps.
