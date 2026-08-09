#!/usr/bin/env python3
"""Validate BuildCraft shared/legacy/modern source layers and deduplication."""
from __future__ import annotations

from hashlib import sha256
from pathlib import Path
import sys

from source_layout import ROOT, family_targets, load_properties, target_layout


def fail(message: str) -> None:
    print(f"ERROR: {message}", file=sys.stderr)
    raise SystemExit(1)


def digest(path: Path) -> bytes:
    h = sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            h.update(chunk)
    return h.digest()


def file_map(root: Path) -> dict[str, Path]:
    src = root / "src"
    if not src.is_dir():
        return {}
    return {
        p.relative_to(root).as_posix(): p
        for p in src.rglob("*") if p.is_file()
    }


def identical_same_path(left: dict[str, Path], right: dict[str, Path]) -> list[str]:
    return sorted(
        rel for rel in set(left) & set(right)
        if digest(left[rel]) == digest(right[rel])
    )


def main() -> None:
    props = load_properties()
    configured_families = [x.strip() for x in props.get("sourceFamilies", "").split(",") if x.strip()]
    if configured_families != ["legacy", "modern"]:
        fail(f"sourceFamilies must be legacy,modern; got {configured_families}")

    grouped = family_targets(props)
    expected_legacy = ["1.19.2-forge", "1.20.1-forge"]
    if grouped.get("legacy") != expected_legacy:
        fail(f"legacy: expected maintenance targets {expected_legacy}, got {grouped.get('legacy')}")
    required_modern = {"1.21.1-forge", "1.21.1-neoforge"}
    modern_targets = grouped.get("modern", [])
    if not required_modern.issubset(modern_targets):
        fail(f"modern: required targets {sorted(required_modern)} are not all present: {modern_targets}")

    reference = props.get("behaviorReference")
    if reference != "1.19.2-forge":
        fail(f"behaviorReference must remain 1.19.2-forge, got {reference!r}")

    layouts = [target_layout(target, props) for targets in grouped.values() for target in targets]
    shared_roots = {layout.shared_root for layout in layouts}
    if len(shared_roots) != 1:
        fail("all targets must use exactly one global shared source root")
    shared_root = next(iter(shared_roots))
    if not shared_root.is_dir():
        fail(f"missing global shared source root {shared_root.relative_to(ROOT)}")
    shared_map = file_map(shared_root)
    if not (shared_root / "src/main/java").is_dir() or not (shared_root / "src/main/resources").is_dir():
        fail("global shared root must contain src/main/java and src/main/resources")

    physical_files = len(shared_map)
    effective_files = 0
    family_reports: list[str] = []
    family_maps: dict[str, dict[str, Path]] = {}

    for family, targets in grouped.items():
        family_layouts = [target_layout(target, props) for target in targets]
        family_roots = {layout.family_root for layout in family_layouts}
        if len(family_roots) != 1:
            fail(f"{family}: targets do not share exactly one family source root")
        family_root = next(iter(family_roots))
        if not family_root.is_dir():
            fail(f"{family}: missing family source root {family_root.relative_to(ROOT)}")
        family_map = file_map(family_root)
        family_maps[family] = family_map

        shared_family_overlap = sorted(set(shared_map) & set(family_map))
        if shared_family_overlap:
            fail(
                f"{family}: files overlap global shared and family layers; "
                f"first: {shared_family_overlap[0]}"
            )

        overlay_maps: dict[str, dict[str, Path]] = {}
        for layout in family_layouts:
            if not layout.overlay_root.is_dir():
                fail(f"{layout.target}: missing overlay {layout.overlay_root.relative_to(ROOT)}")
            overlay = file_map(layout.overlay_root)
            overlap = sorted((set(shared_map) | set(family_map)) & set(overlay))
            if overlap:
                fail(
                    f"{layout.target}: files overlap shared/family and overlay layers; "
                    f"first: {overlap[0]}"
                )
            overlay_maps[layout.target] = overlay

        # A file that exists with identical bytes in every target overlay belongs
        # one level up in the family layer. For future 1.21.11/26.x targets this
        # still allows two-target subgroups to diverge without forcing conditionals.
        if targets:
            common_overlay_paths = set.intersection(*(set(overlay_maps[t]) for t in targets))
            escaped = []
            for rel in sorted(common_overlay_paths):
                hashes = {digest(overlay_maps[t][rel]) for t in targets}
                if len(hashes) == 1:
                    escaped.append(rel)
            if escaped:
                fail(
                    f"{family}: {len(escaped)} identical files remain duplicated in every overlay; "
                    f"move them to {family_root.relative_to(ROOT)} (first: {escaped[0]})"
                )

        physical_files += len(family_map) + sum(len(overlay_maps[t]) for t in targets)
        effective_family = sum(
            len(shared_map) + len(family_map) + len(overlay_maps[t])
            for t in targets
        )
        effective_files += effective_family
        family_reports.append(
            f"{family}: family-shared={len(family_map)}, overlays="
            + "/".join(f"{t}:{len(overlay_maps[t])}" for t in targets)
        )

    # Anything identical at the same relative path in both family layers is
    # valid for all four current targets and therefore belongs in source-shared.
    escaped_global = identical_same_path(family_maps["legacy"], family_maps["modern"])
    if escaped_global:
        fail(
            f"{len(escaped_global)} cross-family identical files escaped source-shared; "
            f"first: {escaped_global[0]}"
        )

    saved_files = effective_files - physical_files
    reduction = (saved_files / effective_files * 100.0) if effective_files else 0.0
    print(
        f"Source family layout OK: global-shared={len(shared_map)}; "
        + "; ".join(family_reports)
    )
    print(
        f"Physical source files={physical_files}, effective per-target files={effective_files}, "
        f"duplicate copies eliminated={saved_files} ({reduction:.1f}% of former target copies)"
    )


if __name__ == "__main__":
    main()
