# Mutual Transfer — Hospital Dataset Scope

## Decision

For the Mutual Transfer MVP, the Firestore `hospitals` collection is scoped to the **hospital-type records actually represented in the detailed regional listings** of the Sri Lanka Ministry of Health and Mass Media 2026 publication.

The project decision is now to use the **1,205 hospital-type records represented by the detailed regional listings**.

The five separately listed **Arogya Center** records remain excluded from transfer destinations and remain source-reference inventory only.

## Source-authority rule

The detailed regional tables are authoritative for the assembled hospital record inventory.

The publication summary contains arithmetic/category-count discrepancies relative to the detailed tables. We will not invent an additional institution or force the detailed tables to match a summary total.

The final dataset target is therefore **exactly 1,205 hospital-type records**.

## Manually reconciled source details

The following source-layout issues were manually reconciled against the original PDF:

- Page 7 contains both **DHA Moratuwa** and **DHA Wethara**.
- Page 9 lists **DGH Negambo** and **DGH Gampaha**; **DGH Gampaha is Provincial Ministry**.
- Page 28 contains **PMCU Kurumpasiddy (Instead of Palali)**.
- Page 29 contains **PMCU Piramanthanaru (Elephantpass)**.
- Page 30 contains **DGH Mullaitivu** and **BHA Mankulam**.
- Page 45 contains **TH Anuradhapura**.
- Page 50 contains **TH Badulla**.

These are source reconciliation corrections, not newly invented records.

## Excluded Arogya Centers

The detailed 2026 publication lists these separately:

- Thalpitiya — Kalutara
- Polgollawatta — Kandy
- Dankanda — Matale
- Mapalagama — Galle
- Ethoya — Ratnapura

They remain source-reference inventory only and are not transfer destinations.

## Data-quality rules

- Do not invent HINs.
- Do not invent coordinates.
- Coordinates are nullable until independently validated.
- Missing coordinates must never be converted to `0.0, 0.0`.
- Preserve Ministry functional-status remarks.
- Non-functioning institutions must not be offered as active transfer destinations.
- `hospitalId` is the immutable application identifier; matching does not use display names.
- Preserve provenance: Ministry source year/reference, coordinate source, verification date, and dataset version.
- The canonical dataset must contain exactly **1,205 detailed hospital-type records** before Firestore import.

## Source

Sri Lanka Ministry of Health and Mass Media, Directorate of Planning, **List of Hospitals — Updated 2026**.

Official source:
https://www.health.gov.lk/wp-content/uploads/2025/06/Institution-List-2026-Updated.pdf
