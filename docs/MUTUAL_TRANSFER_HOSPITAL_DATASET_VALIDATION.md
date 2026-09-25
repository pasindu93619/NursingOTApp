# Mutual Transfer Hospital Dataset Validation — 2026 Ministry Source

## Status

**1,206 detailed hospital-type records selected for the MVP dataset.**

The official Ministry of Health and Mass Media **List of Hospitals — Updated 2026** remains the source of truth.

Project scope:
- Firestore `hospitals` collection: hospital-type records from the detailed regional listings.
- **Target inventory: 1,206 records.**
- Arogya Centers: source-reference inventory only, not transfer destinations.
- No invented HINs.
- No unvalidated coordinates.
- No `0.0, 0.0` coordinate fallback.

## Source discrepancy — project decision

The Ministry summary arithmetic gives 1,205 hospital-type records, while the detailed regional listings represent 1,206 hospital-type records.

The discrepancy is associated with Base Hospital Type-B (BHB): the summary reports 45, while the detailed regional BHB declarations sum to 46.

**Project decision:** use the **detailed regional listings as authoritative for the hospital record inventory**, giving a target of **1,206 hospital-type records**.

No detailed hospital record will be deleted or reclassified solely to force the summary count.

## Detailed source categories

The detailed regional pages contain:

- NH: 3
- TH: 12
- DGH: 20
- BHA: 35
- BHB: 46
- DHA: 66
- DHB: 147
- DHC: 279
- PMCU: 577
- Specialized Teaching Hospital: 5
- Other Specialized Hospital: 14
- Board Managed Hospital entries: 2

The coded categories total 1,185; the additional specialized and board-managed entries bring the detailed hospital-type inventory to **1,206**.

## Extraction status

A coordinate-aware PDF extraction prototype has been run against the 60-page Ministry PDF.

Some source-layout variants still require explicit handling before the dataset is committed. These are extraction issues only; no records are being invented.

Current prototype coverage:
- NH: 2/3
- TH: 12/12
- DGH: 20/20
- BHA: 34/35
- BHB: 46/46
- DHA: 66/66
- DHB: 147/147
- DHC: 277/279
- PMCU: 574/577

The final dataset must not be committed until these remaining source-layout records are reconciled against the original PDF.

## Coordinates and HINs

Coordinates are not yet populated.

HINs are not yet populated unless independently verified.

The Kotlin contract intentionally allows nullable HIN/coordinates. Matching must never treat missing coordinates as `0.0, 0.0`.

## Dataset commit gate

- [x] 1,206 detailed-record scope selected
- [x] 1205-vs-1206 discrepancy resolved by explicit project decision
- [ ] all 1,206 source records extracted and reconciled
- [ ] category counts validated against the final records
- [ ] duplicates reviewed
- [ ] RDHS/province fields validated
- [ ] functional status/remarks preserved
- [ ] deterministic hospital IDs generated
- [ ] HINs independently verified where available
- [ ] coordinates independently validated
- [ ] provenance/version fields populated
- [ ] Firestore import payload validated

## Source

Sri Lanka Ministry of Health and Mass Media, Directorate of Planning, **List of Hospitals — Updated 2026**.
