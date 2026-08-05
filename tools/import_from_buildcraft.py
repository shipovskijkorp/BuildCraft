#!/usr/bin/env python3
"""Synchronize non-English BuildCraft resources into the localization addon."""
from __future__ import annotations

import argparse
import shutil
from pathlib import Path


def replace_directory(source: Path, destination: Path, excluded: set[str]) -> int:
    destination.mkdir(parents=True, exist_ok=True)
    for old in destination.glob("*.json"):
        old.unlink()
    count = 0
    for file in sorted(source.glob("*.json")):
        if file.name in excluded:
            continue
        shutil.copy2(file, destination / file.name)
        count += 1
    return count


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("buildcraft", type=Path, help="BuildCraft repository root or extracted source root")
    args = parser.parse_args()

    root = args.buildcraft.resolve()
    resources = root / "src/main/resources"
    if not resources.is_dir() and (root / "main/resources").is_dir():
        resources = root / "main/resources"
    if not resources.is_dir():
        raise SystemExit(f"Cannot find src/main/resources below {root}")

    project = Path(__file__).resolve().parents[1]
    ordinary = replace_directory(
        resources / "assets/buildcraft/lang",
        project / "src/main/resources/assets/buildcraft/lang",
        {"en_us.json"},
    )
    guide = replace_directory(
        resources / "assets/buildcraft/guide/text",
        project / "src/main/resources/assets/buildcraft/guide/text",
        {"en_us.json"},
    )
    print(f"Imported {ordinary} ordinary locales and {guide} Guide Book locale packs")


if __name__ == "__main__":
    main()
