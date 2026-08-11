#!/usr/bin/env python3
"""Fail when BCCE implementation imports an unmapped legacy BuildCraft API symbol."""
from __future__ import annotations

import csv
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAP = ROOT / "docs/api2/LEGACY_IMPORT_MIGRATION_MAP.csv"
SOURCE_ROOTS = (
    "source-shared/src/main/java",
    "source-families/legacy/src/main/java",
    "source-families/modern/src/main/java",
    "source-platforms/forge/src/main/java",
    "source-platforms/neoforge/src/main/java",
    "version-src/1.19.2-forge/src/main/java",
    "version-src/1.20.1-forge/src/main/java",
    "version-src/1.21.1-neoforge/src/main/java",
)
IMPORT = re.compile(r"^\s*import\s+(buildcraft\.api\.(?!v2\.)[^;]+);", re.MULTILINE)


def implementation_imports() -> set[str]:
    found: set[str] = set()
    for relative in SOURCE_ROOTS:
        source_root = ROOT / relative
        if not source_root.exists():
            continue
        for path in source_root.rglob("*.java"):
            relative_path = path.relative_to(source_root).as_posix()
            if relative_path.startswith("buildcraft/api/"):
                continue
            found.update(IMPORT.findall(path.read_text(encoding="utf-8", errors="ignore")))
    return found


def main() -> int:
    with MAP.open(encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle))
    mapped = {row["legacy_import"]: row for row in rows}
    current = implementation_imports()

    missing = sorted(current - mapped.keys())
    invalid = sorted(
        symbol for symbol in current
        if symbol in mapped
        and mapped[symbol]["disposition"] not in {"MIGRATE", "INTERNALIZE"}
    )
    blank = sorted(
        symbol for symbol in current
        if symbol in mapped and not mapped[symbol]["v2_replacement_or_action"].strip()
    )

    if missing or invalid or blank:
        if missing:
            print("Unmapped legacy API imports:")
            for symbol in missing:
                print(f"  {symbol}")
        if invalid:
            print("Legacy API imports with invalid disposition:")
            for symbol in invalid:
                print(f"  {symbol}: {mapped[symbol]['disposition']}")
        if blank:
            print("Legacy API imports without a migration action:")
            for symbol in blank:
                print(f"  {symbol}")
        return 1

    stale = sorted(mapped.keys() - current)
    print(
        f"API v2 migration surface OK: {len(current)} implementation legacy imports mapped "
        f"({sum(mapped[s]['disposition'] == 'MIGRATE' for s in current)} migrate, "
        f"{sum(mapped[s]['disposition'] == 'INTERNALIZE' for s in current)} internalize; "
        f"{len(stale)} historical map entries)"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
