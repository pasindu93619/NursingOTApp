# Mutual Transfer — Hospital Dataset Scope

## Decision

For the Mutual Transfer MVP, the Firestore `hospitals` collection is scoped to the **hospital-type records in the detailed regional listings** of the Ministry of Health 2026 publication.

The project decision is now to use the **1,206 hospital-type records represented by the detailed regional listings**, rather than forcing the dataset to the publication summary's 1,205 arithmetic.

The five separately listed **Arogya Center** records remain excluded from transfer destinations and remain source-reference inventory only.

## Why 1,206

The Ministry summary arithmetic gives 1,205 hospital-type records, but the detailed regional listings contain one additional hospital-type record, producing 1,206.

The discrepancy is specifically associated with the detailed Base Hospital Type-B (BHB) listings: the summary reports 45, while the detailed regional BHB (n) declarations sum to 46.

For this project, **the detailed regional listings are authoritative for the hospital record inventory**.

We will not delete, reclassify, or duplicate a detailed hospital record merely to force the summary count.

## Excluded Arogya Centers

The detailed 2026 publication lists:

- Thalpitiya — Kalutara
- Polgollawatta — Kandy
- Dankanda — Matale
- Mapalagama — Galle
- Ethoya — Ratnapura

These remain source-reference inventory only and are not transfer destinations.

## Data-quality rules

- Do not invent HINs.
- Do not invent coordinates.
- Coordinates are nullable until independently validated.
- Missing coordinates must never be converted to `0.0, 0.0`.
- Preserve Ministry functional-status remarks.
- Non-functioning institutions must not be offered as active transfer destinations.
- `hospitalId` is the immutable application identifier; matching does not use display names.
- Preserve provenance: Ministry source year/reference, coordinate source, verification date, and dataset version.
- The final dataset must contain exactly **1,206 detailed hospital-type records** before Firestore import.

## Source

Sri Lanka Ministry of Health and Mass Media, Directorate of Planning, **List of Hospitals — Updated 2026**.

Official source:
https://www.health.gov.lk/wp-content/uploads/2025/06/Institution-List-2026-Updated.pdf
