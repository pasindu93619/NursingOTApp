# Mutual Transfer Hospital Dataset Validation — 2026 Ministry Source

## Status

**BLOCKED BEFORE DATASET COMMIT**

The official Ministry of Health and Mass Media **List of Hospitals — Updated 2026** is the authoritative source selected for the Mutual Transfer hospital reference dataset.

The project scope is locked to **Option C**:
- Firestore `hospitals` collection: hospital-type records only.
- Arogya Centers: auxiliary/source-reference inventory, not transfer destinations.
- No invented HINs.
- No unvalidated coordinates.
- No `0.0, 0.0` coordinate fallback.

## Source summary

The Ministry publication summary reports:
- Tertiary Care: 55
- Secondary Care: 81
- Primary Care institutions: 1074
- Hospital-type total from those categories: **1205**
- Publication detailed total: **1210**

The five Arogya Centers account for the documented difference between the 1205 hospital-type scope and the publication total of 1210.

## Validation performed

A coordinate-aware PDF extraction prototype was run against the uploaded 60-page Ministry PDF.

The detailed type labels in the regional pages sum to:
- NH: 3
- TH: 12
- DGH: 20
- BHA: 35
- BHB: **46**
- DHA: 66
- DHB: 147
- DHC: 279
- PMCU: 577

These coded hospital-type entries total **1185**.

The detailed pages also contain:
- Specialized Teaching Hospital: 5
- Other Specialized Hospital: 14
- Board Managed Hospital entries: 2 (one tertiary-care Board Managed Hospital and one Board Managed Hospital (Vijaya KH))

Therefore the detailed regional pages appear to represent **1206 hospital-type entries**, not 1205.

## Source discrepancy

The Ministry summary states **Base Hospital Type-B = 45**.

The detailed regional pages' `BHB (n)` labels sum to **46**.

The summary separately lists:
- Board Managed Hospital: 1
- Board Managed Hospital (Vijaya KH): 1

The detailed pages also show two Board Managed Hospital entries, including Sri Jayawardanapura General Hospital and Wijaya Kumaratunga Memorial Hospital, Seeduwa.

This creates a **one-record discrepancy** between the publication summary arithmetic and the detailed regional listings.

### Do not resolve this by guessing

The app must not arbitrarily remove, reclassify, or duplicate a hospital to force the count to 1205.

The correct resolution requires either:
1. an authoritative clarification/correction to the Ministry source, or
2. an explicit project decision by Pasindu to define which source representation is authoritative for this discrepancy.

## Extraction prototype result

The first coordinate-aware extraction pass successfully recovered the coded hospital records with the following totals:

- NH: 2 extracted; 1 source-layout variant still requires explicit handling
- TH: 12
- DGH: 20
- BHA: 34; 1 source-layout variant still requires explicit handling
- BHB: 46
- DHA: 66
- DHB: 147
- DHC: 277; 2 source-layout variants still require explicit handling
- PMCU: 574; 3 source-layout variants still require explicit handling

The missing records are extraction-layout issues, not permission to invent records. They must be resolved against the original PDF before the dataset is committed.

## Coordinates and HINs

Coordinates are **not yet populated**.

HINs are **not yet populated** unless independently verified.

The current Kotlin contract intentionally allows nullable coordinates and HINs. Matching must not treat missing coordinates as (0,0).

## Dataset commit gate

The hospital dataset must not be committed to the app/Firestore until all of the following are true:

- [ ] authoritative 1205-vs-1206 discrepancy resolved
- [ ] exact hospital-type record count locked
- [ ] all source records extracted and reconciled
- [ ] category counts validated
- [ ] duplicates reviewed
- [ ] RDHS/province fields validated
- [ ] functional status/remarks preserved
- [ ] deterministic hospital IDs generated
- [ ] HINs independently verified where available
- [ ] coordinates independently validated
- [ ] provenance/version fields populated
- [ ] Firestore import payload validated

## Important

This document intentionally records the blocker rather than silently changing the Ministry data to fit the application's expected count.
