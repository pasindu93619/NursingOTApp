# Mutual Transfer Hospital Dataset — 2026 Workbook Validation

Source workbook: Sri_Lanka_Hospitals_List_2026.xlsx
Canonical source sheet: All Hospitals
Validation date: 2026-09-25

## Counts

Total hospital records in workbook: 1,216

Included Mutual Transfer hospital-type records: **1,206**

Excluded:
- Arogya Center: 5
- Special Campaign / Other: 3
- Other Hospitals: 2

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

Included total: **1,206**

## Authority

The supplied workbook now provides an explicit Authority column for every included hospital record.

The canonical dataset therefore preserves the supplied Authority value exactly:
- Line Ministry
- Provincial Ministry

No additional authority inference is required for the canonical dataset.

## Fields intentionally not required

For this Mutual Transfer reference dataset, the project does not require:
- HIN
- latitude
- longitude
- a separately inferred functional-status field

These fields will not block dataset completion.

The workbook Remarks field is retained because it is source information and may contain operational notes.

## Integrity checks

- Source row numbers are unique across included records.
- Institution names are not assumed to be unique.
- Authority is present for all 1,206 included records.
- No HIN values are invented.
- No coordinates are invented.
- Workbook Remarks values are preserved.
- The five Arogya Centers are excluded from transfer destinations.
- Special Campaign / Other and Other Hospitals are excluded from transfer destinations.

## Traceability

Each canonical record uses hospitalId = MOH2026-<source row number> and retains sourceRow.

Canonical working artifact:
Sri_Lanka_Hospitals_2026_Canonical_1206.csv

The canonical dataset is reference data only and is not yet a Firestore seed.
