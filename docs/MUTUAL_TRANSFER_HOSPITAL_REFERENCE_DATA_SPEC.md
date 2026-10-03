# Mutual Transfer — Hospital Reference Data Specification

## Authority

Primary institutional source: Sri Lanka Ministry of Health and Mass Media, Directorate of Planning, **List of Hospitals — Updated 2026**.

The 2026 publication states that functional status was updated from information provided by relevant RDHS planning units and that the information was submitted and verified by those planning units.

## Dataset rule

This is a one-time assembled reference dataset, not a live public API.

Do not invent hospital records, coordinates, HIN values, categories, or operational status.

## Canonical record

Each record must contain:

- hospitalId — immutable application identifier
- hin — Ministry Health Institution Number when verified; nullable until authoritative mapping is available
- name — Ministry-listed institution name
- category — Ministry-listed hospital/institution category
- administeringAuthority — Line Ministry or Provincial Ministry
- province
- district / RDHS district
- latitude
- longitude
- functionalStatus
- remarks
- sourceYear
- sourceReference

## Identifier policy

The matching engine must use hospitalId, never a display name.

Where an authoritative HIN is available and verified, preserve it in hin. Do not fabricate an HIN.

If an institution has no verified HIN mapping in the assembled source material, it remains eligible for dataset assembly only after a deterministic internal ID is assigned and the provenance is retained.

## Status policy

The Ministry's operational remarks must be preserved.

Examples in the 2026 publication include institutions marked Not Functioning and institutions whose listed category is reported as functioning as another category.

A non-functioning institution must not be offered as an active transfer destination.

A category-change remark must not be silently discarded.

## Coordinate policy

Coordinates are secondary reference data used for distance calculations.

Coordinate source may be OpenStreetMap/Nominatim or another explicitly documented free source, but coordinates must be matched to the Ministry institution record and checked for:

- correct institution
- correct locality
- duplicate coordinates
- implausible country/region
- obvious name collision

Do not treat a geocoder result as authoritative institutional identity.

## Release/provenance

The assembled dataset must retain:

- Ministry source year: 2026
- Ministry source URL/reference
- coordinate source
- coordinate verification date
- dataset version

## Firestore shape

Collection: hospitals/{hospitalId}

The Firestore reference collection is read-only to normal nurse clients. Administrative corrections belong to the later Mutual Transfer trust/scale phase.

## Local Room policy

Hospital reference data is reference/cache data only. It must not alter the existing OT, salary, financial, or clinical tables.

Any future Room hospital cache must use an additive migration and preserve existing data.

## Matching safety

Distance-based matching must only use records with validated coordinates. Missing or suspect coordinates must not silently become (0.0, 0.0) because that would create false distance calculations.

## Current source

Official Ministry 2026 hospital publication:
https://www.health.gov.lk/wp-content/uploads/2025/06/List-of-Hospitals-2026-compressed.pdf

Official Ministry HIN page:
https://www.health.gov.lk/moh-page/health-institutions-number-hin/

This specification does not claim that every 2026 institution has yet been mapped to an HIN or validated coordinate. That mapping remains a dataset-assembly task.