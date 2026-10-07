#!/usr/bin/env python3
"""
NursingOTApp — canonical 2026 hospital asset repair.

SAFE MODE:
- Reads the already-committed canonical 1206-row CSV from Git commit 1406db4.
- Does NOT modify Kotlin, Room, Firebase, UI, or business logic.
- Refuses to replace assets unless the source validates to exactly 1206 unique records.
- Rebuilds the seven JSONL.GZIP asset partitions expected by the current repository.
"""

from __future__ import annotations

import csv
import gzip
import hashlib
import json
import os
import shutil
import subprocess
import tempfile
from pathlib import Path

COMMIT = "1406db4"
CSV_PATH = "app/src/main/assets/hospitals_2026/hospitals_2026.csv"
ASSET_DIR_REL = Path("app/src/main/assets/hospitals_2026")
PART_SIZES = [173, 173, 200, 173, 173, 173, 141]
EXPECTED = 1206
DATASET_VERSION = "MOH-2026-1206"
SOURCE_YEAR = 2026
SOURCE_REFERENCE = "Sri Lanka Hospitals List 2026 - All Hospitals"

def run(*args: str) -> bytes:
    return subprocess.check_output(args)

def repo_root() -> Path:
    return Path(run("git", "rev-parse", "--show-toplevel").decode().strip())

def canonical_csv(root: Path) -> bytes:
    return run("git", "show", f"{COMMIT}:{CSV_PATH}")

def parse_csv(raw: bytes):
    text = raw.decode("utf-8-sig")
    lines = text.splitlines()

    # Canonical file starts with a metadata line, then the CSV header.
    if not lines or not lines[0].startswith("TAB NAME:"):
        raise RuntimeError("Canonical CSV header marker not found.")

    csv_text = "\n".join(lines[1:])
    reader = csv.DictReader(csv_text.splitlines())

    required = {
        "index", "hospitalId", "sourceRow", "Province", "RDHS Division",
        "category", "categoryFullName", "name", "Authority", "Remarks"
    }
    if not required.issubset(set(reader.fieldnames or [])):
        raise RuntimeError(
            f"Canonical CSV columns are incomplete. Found: {reader.fieldnames}"
        )

    rows = list(reader)
    if len(rows) != EXPECTED:
        raise RuntimeError(
            f"REFUSING REPAIR: canonical CSV contains {len(rows)} records; "
            f"expected exactly {EXPECTED}."
        )

    ids = [r["hospitalId"].strip() for r in rows]
    if len(set(ids)) != EXPECTED:
        raise RuntimeError("REFUSING REPAIR: duplicate hospitalId in canonical CSV.")

    source_rows = [int(r["sourceRow"]) for r in rows]
    if source_rows != list(range(1, EXPECTED + 1)):
        raise RuntimeError("REFUSING REPAIR: sourceRow sequence is not 1..1206.")

    return rows

def to_json(row):
    def clean(v):
        return (v or "").strip()

    return {
        "hospitalId": clean(row["hospitalId"]),
        "province": clean(row["Province"]),
        "rdhsDivision": clean(row["RDHS Division"]),
        "category": clean(row["category"]),
        "categoryFullName": clean(row["categoryFullName"]),
        "name": clean(row["name"]),
        "administeringAuthority": clean(row["Authority"]),
        "remarks": clean(row["Remarks"]) or None,
        "sourceYear": SOURCE_YEAR,
        "sourceReference": SOURCE_REFERENCE,
        "datasetVersion": DATASET_VERSION,
    }

def write_part(path: Path, objects):
    with gzip.open(path, "wb", compresslevel=9, mtime=0) as gz:
        for obj in objects:
            line = json.dumps(
                obj,
                ensure_ascii=False,
                separators=(",", ":"),
            ).encode("utf-8") + b"\n"
            gz.write(line)

def validate_part(path: Path):
    records = []
    with gzip.open(path, "rt", encoding="utf-8") as gz:
        for line in gz:
            line = line.strip()
            if not line:
                continue
            obj = json.loads(line)
            records.append(obj)

    return records

def main():
    root = repo_root()
    asset_dir = root / ASSET_DIR_REL

    print(f"Repository: {root}")
    print(f"Source commit: {COMMIT}")
    print(f"Asset directory: {asset_dir}")

    raw = canonical_csv(root)
    rows = parse_csv(raw)

    print(f"Canonical CSV records: {len(rows)}")
    print(f"Canonical CSV SHA256: {hashlib.sha256(raw).hexdigest()}")

    objects = [to_json(r) for r in rows]

    # Validate before touching the working tree.
    if len(objects) != EXPECTED:
        raise RuntimeError("Internal record-count validation failed.")

    if len({o["hospitalId"] for o in objects}) != EXPECTED:
        raise RuntimeError("Internal hospitalId uniqueness validation failed.")

    if any(o["datasetVersion"] != DATASET_VERSION for o in objects):
        raise RuntimeError("Internal datasetVersion validation failed.")

    # Verify intended deterministic partitioning.
    if sum(PART_SIZES) != EXPECTED:
        raise RuntimeError("PART_SIZES do not sum to 1206.")

    # Create temporary output outside the asset directory.
    tmp = Path(tempfile.mkdtemp(prefix="nursing_hospital_repair_"))
    backup = asset_dir / "_backup_before_canonical_repair"
    tmp_parts = []

    try:
        start = 0
        for i, size in enumerate(PART_SIZES, 1):
            end = start + size
            part = tmp / f"part{i:02d}.jsonl.gz.data"
            write_part(part, objects[start:end])

            check = validate_part(part)
            if len(check) != size:
                raise RuntimeError(
                    f"Part {i:02d} validation failed: {len(check)} != {size}"
                )

            tmp_parts.append(part)
            print(f"part{i:02d}: {len(check)} records, {part.stat().st_size} bytes")
            start = end

        # Global validation of generated partitions.
        merged = []
        for p in tmp_parts:
            merged.extend(validate_part(p))

        if len(merged) != EXPECTED:
            raise RuntimeError(
                f"Generated bundle contains {len(merged)} records, not {EXPECTED}."
            )

        ids = [x["hospitalId"] for x in merged]
        if len(set(ids)) != EXPECTED:
            raise RuntimeError("Generated bundle contains duplicate hospitalId values.")

        if ids != [o["hospitalId"] for o in objects]:
            raise RuntimeError("Generated bundle order differs from canonical CSV.")

        # Only now back up and replace the seven broken assets.
        backup.mkdir(exist_ok=True)
        for i in range(1, 8):
            dest = asset_dir / f"part{i:02d}.jsonl.gz.data"
            if dest.exists():
                shutil.copy2(dest, backup / dest.name)

        for part in tmp_parts:
            shutil.copy2(part, asset_dir / part.name)

        print()
        print("REPAIR APPLIED SUCCESSFULLY")
        print(f"Total records: {len(merged)}")
        print(f"Unique hospitalId: {len(set(ids))}")
        print(f"Dataset version: {DATASET_VERSION}")
        print(f"Backup: {backup}")

    finally:
        shutil.rmtree(tmp, ignore_errors=True)

if __name__ == "__main__":
    main()
