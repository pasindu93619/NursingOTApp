# Mutual Transfer — Hospital Dataset Scope

## Decision

For the Mutual Transfer MVP, the canonical hospital reference source is the supplied workbook:

**Sri_Lanka_Hospitals_List_2026.xlsx**

The workbook's **All Hospitals** sheet contains 1,216 records.

For the Mutual Transfer hospital destination dataset, use the **1,206 hospital-type records** in that sheet.

The following 10 records are excluded from transfer destinations:

- 5 Arogya Center records
- 3 Special Campaign / Other records
- 2 Other Hospitals records

Therefore:

**1,216 total workbook records - 10 excluded records = 1,206 hospital-type records.**

The workbook is now the structured source for the canonical dataset. The earlier 1,205 PDF-summary arithmetic is not used to delete a detailed hospital record.

## Source corrections confirmed

The supplied workbook independently contains the previously reconciled records:

- DHA Moratuwa
- DHA Wethara
- DGH Gampaha
- PMCU Kurumpasiddy (Instead of Palali)
- PMCU Piramanthanaru (Elephantpass)
- BHA Mankulam
- DGH Mullaitivu
- TH Anuradhapura
- TH Badulla

DGH Gampaha is listed under RDHS Gampaha. Authority classification must not be invented from the workbook where the workbook does not provide an explicit authority field; such mapping remains a separate verified enrichment step.

## Data-quality rules

- Do not invent HINs.
- Do not invent coordinates.
- Coordinates are nullable until independently validated.
- Missing coordinates must never become 0.0, 0.0.
- Preserve the workbook's Remarks field exactly.
- Do not silently assign functional status when the source does not explicitly provide it.
- hospitalId is an immutable application identifier.
- Matching must use hospitalId, never display names.
- Preserve source row number for traceability.
- Preserve source year and dataset version.
- Do not import excluded Arogya Center, Special Campaign / Other, or Other Hospitals records into the transfer destination collection.

## Canonical dataset target

**Exactly 1,206 hospital-type records.**

The canonical working artifact is generated from the supplied workbook and retains the source row number as sourceRow.

## Source

Sri Lanka Ministry of Health and Mass Media, Directorate of Planning, **List of Hospitals — Updated 2026**, supplied in structured workbook form as Sri_Lanka_Hospitals_List_2026.xlsx.