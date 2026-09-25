# Mutual Transfer — Hospital Dataset Scope

## Decision

For the Mutual Transfer MVP, the Firestore `hospitals` collection is scoped to the **hospital-type records** in the Ministry of Health 2026 publication.

The five separately listed **Arogya Center** records are not transfer-destination records in the MVP hospital collection. They remain part of the source inventory for future reference-data work.

## Current source-derived count

The Ministry summary lists these hospital-type totals:

- Tertiary Care: 55
- Secondary Care: 81
- Primary Care: 1,074
- Sum of the listed hospital-type categories: **1,205**

The publication's overall detailed total is 1,210. The detailed regional pages additionally contain five Arogya Center entries. The MVP therefore deliberately uses the 1,205 hospital-type records rather than forcing the Arogya Centers into the transfer-destination collection.

## Excluded Arogya Centers

The detailed 2026 publication lists:

- Thalpitiya — Kalutara
- Polgollawatta — Kandy
- Dankanda — Matale
- Mapalagama — Galle
- Ethoya — Ratnapura

These are retained as source-reference inventory only and are not silently deleted from the Ministry source.

## Data-quality rules

- Do not invent HINs.
- Do not invent coordinates.
- Coordinates are nullable until independently validated.
- Missing coordinates must never be converted to `0.0, 0.0`.
- Preserve Ministry functional-status remarks.
- Non-functioning institutions must not be offered as active transfer destinations.
- `hospitalId` is the immutable application identifier; matching does not use display names.
- Preserve provenance: Ministry source year/reference, coordinate source, verification date, and dataset version.

## Source

Sri Lanka Ministry of Health and Mass Media, Directorate of Planning, **List of Hospitals — Updated 2026**.

Official source:
https://www.health.gov.lk/wp-content/uploads/2025/06/Institution-List-2026-Updated.pdf
