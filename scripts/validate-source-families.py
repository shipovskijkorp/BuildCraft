#!/usr/bin/env python3
"""Validate BuildCraft's shared/family/platform/target hybrid source layout."""
from __future__ import annotations

from collections import defaultdict
from hashlib import sha256
from pathlib import Path
import re
import sys

from source_layout import (
    ROOT,
    evaluate_condition,
    family_targets,
    generation_targets,
    load_properties,
    platform_targets,
    preprocess_text,
    target_layout,
    validate_all_directives,
)


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
    return {p.relative_to(root).as_posix(): p for p in src.rglob("*") if p.is_file()}


def identical_same_path(left: dict[str, Path], right: dict[str, Path]) -> list[str]:
    return sorted(rel for rel in set(left) & set(right) if digest(left[rel]) == digest(right[rel]))


def validate_loader_boundaries(shared_root: Path, family_roots: dict[str, Path], platform_roots: dict[str, Path]) -> None:
    forbidden = ("net.minecraftforge", "net.neoforged", "net.fabricmc")
    for root in [shared_root, *family_roots.values()]:
        for path in root.rglob("*.java"):
            text = path.read_text(encoding="utf-8", errors="replace")
            for token in forbidden:
                if token in text:
                    fail(f"loader API {token!r} escaped platform layer: {path.relative_to(ROOT)}")

    platform_forbidden = {
        "forge": ("net.neoforged", "net.fabricmc"),
        "neoforge": ("net.minecraftforge", "net.fabricmc"),
        "fabric": ("net.minecraftforge", "net.neoforged"),
    }
    for platform, root in platform_roots.items():
        for path in root.rglob("*.java"):
            text = path.read_text(encoding="utf-8", errors="replace")
            for token in platform_forbidden.get(platform, ()):
                if token in text:
                    fail(f"{platform} platform source imports {token}: {path.relative_to(ROOT)}")


def validate_condition_policy(roots: list[Path], overlays: list[Path]) -> tuple[int, int]:
    files = 0
    blocks = 0
    for root in roots:
        for path in root.rglob("*.java"):
            text = path.read_text(encoding="utf-8", errors="strict")
            count = len(re.findall(r"^[ \t]*(?://\?|/\*\?)[ \t]*if\b", text, flags=re.MULTILINE))
            if not count:
                continue
            files += 1
            blocks += count
            if count > 4:
                fail(
                    f"too many inline version branches ({count}) in {path.relative_to(ROOT)}; "
                    "use a family/platform/target implementation instead"
                )
            for line in text.splitlines():
                if re.match(r"^[ \t]*(?://\?|/\*\?)[ \t]*(?:if|}[ \t]*else[ \t]+if)\b", line):
                    if re.search(r"\b(?:forge|neoforge|fabric)\b", line):
                        fail(
                            f"loader condition in gameplay source {path.relative_to(ROOT)}; "
                            "move the difference to source-platforms"
                        )
    for overlay in overlays:
        for path in overlay.rglob("*"):
            if not path.is_file() or path.suffix.lower() not in {".java", ".json", ".toml", ".mcmeta"}:
                continue
            text = path.read_text(encoding="utf-8", errors="replace")
            if "//?" in text or "/*?" in text:
                fail(f"target overlay contains inline conditions: {path.relative_to(ROOT)}")
    return files, blocks




def validate_preprocessor_contract() -> None:
    if not evaluate_condition(">=26 && fabric && modern", minecraft="26.2", family="modern", platform="fabric"):
        fail("version/loader condition engine rejected a valid future modern Fabric target")
    if evaluate_condition("<1.20", minecraft="1.20.1", family="legacy", platform="forge"):
        fail("version condition engine treats Minecraft 1.20.1 as <1.20")

    sample = """//? if <1.20 {
legacyField
//?} else {
/*?
modernMethod()
?*/
//?}
"""
    legacy = preprocess_text(sample, minecraft="1.19.2", family="legacy", platform="forge")
    modern = preprocess_text(sample, minecraft="1.20.1", family="legacy", platform="forge")
    if legacy.strip() != "legacyField" or modern.strip() != "modernMethod()":
        fail("Stonecutter-style branch activation contract is broken")


def main() -> None:
    props = load_properties()
    configured_families = [x.strip() for x in props.get("sourceFamilies", "").split(",") if x.strip()]
    if configured_families != ["legacy", "modern"]:
        fail(f"sourceFamilies must be legacy,modern; got {configured_families}")

    generations = generation_targets(props)
    required_legacy = {"1.19.2-forge", "1.20.1-forge"}
    if not required_legacy.issubset(generations.get("legacy", [])):
        fail(f"legacy build generation is missing reference targets: {generations.get('legacy')}")
    if "1.21.1-neoforge" not in generations.get("modern", []):
        fail(f"modern build generation must contain 1.21.1-neoforge: {generations.get('modern')}")
    if "1.21.1-forge" in {target for values in generations.values() for target in values}:
        fail("1.21.1 Forge is legacy-only and must not return to production source generations")

    if props.get("behaviorReference") != "1.19.2-forge":
        fail("behaviorReference must remain 1.19.2-forge")

    layouts = [target_layout(target, props) for targets in generations.values() for target in targets]
    for layout in layouts:
        for layer in layout.layers:
            if not layer.is_dir():
                fail(f"{layout.target}: missing source layer {layer.relative_to(ROOT)}")
        # Materialization uses shared < family < platform < target precedence.
        # Different overrides are valid; byte-identical redundant overrides are
        # rejected below so precedence does not become an excuse for copies.
        layout.effective_files()

    shared_roots = {layout.shared_root for layout in layouts}
    if len(shared_roots) != 1:
        fail("all targets must use one global shared source root")
    shared_root = next(iter(shared_roots))
    shared_map = file_map(shared_root)

    family_roots: dict[str, Path] = {}
    for family, targets in family_targets(props).items():
        roots = {target_layout(target, props).family_root for target in targets}
        if len(roots) != 1:
            fail(f"{family}: targets use multiple family roots")
        family_roots[family] = next(iter(roots))

    platform_roots: dict[str, Path] = {}
    for platform, targets in platform_targets(props).items():
        roots = {target_layout(target, props).platform_root for target in targets}
        if len(roots) != 1:
            fail(f"{platform}: targets use multiple platform roots")
        platform_roots[platform] = next(iter(roots))

    family_maps = {name: file_map(root) for name, root in family_roots.items()}
    platform_maps = {name: file_map(root) for name, root in platform_roots.items()}
    overlay_maps = {layout.target: file_map(layout.overlay_root) for layout in layouts}

    for family, files in family_maps.items():
        redundant = identical_same_path(shared_map, files)
        if redundant:
            fail(
                f"family/{family}: byte-identical override duplicates source-shared; "
                f"first: {redundant[0]}"
            )

    platform_to_layouts: dict[str, list] = defaultdict(list)
    for layout in layouts:
        platform_to_layouts[layout.platform].append(layout)
    for platform, files in platform_maps.items():
        redundant: list[str] = []
        for relative, platform_path in files.items():
            lower_paths: list[Path] = []
            for layout in platform_to_layouts[platform]:
                lower = None
                for root in (layout.family_root, layout.shared_root):
                    candidate = root / relative
                    if candidate.is_file():
                        lower = candidate
                        break
                if lower is None or digest(lower) != digest(platform_path):
                    break
                lower_paths.append(lower)
            else:
                if lower_paths:
                    redundant.append(relative)
        if redundant:
            fail(
                f"platform/{platform}: byte-identical override duplicates every lower layer; "
                f"first: {sorted(redundant)[0]}"
            )

    for layout in layouts:
        redundant: list[str] = []
        for relative, overlay_path in overlay_maps[layout.target].items():
            lower = None
            for root in (layout.platform_root, layout.family_root, layout.shared_root):
                candidate = root / relative
                if candidate.is_file():
                    lower = candidate
                    break
            if lower is not None and digest(lower) == digest(overlay_path):
                redundant.append(relative)
        if redundant:
            fail(
                f"{layout.target}: target overlay contains a byte-identical lower-layer override; "
                f"first: {sorted(redundant)[0]}"
            )

    escaped_global = identical_same_path(family_maps["legacy"], family_maps["modern"])
    if escaped_global:
        fail(
            f"{len(escaped_global)} identical cross-family files escaped source-shared; "
            f"first: {escaped_global[0]}"
        )

    # Identical target copies inside the same family+platform should move one
    # level up. Conditional files are used only when the bytes really differ.
    groups: dict[tuple[str, str], list[str]] = defaultdict(list)
    for layout in layouts:
        groups[(layout.family, layout.platform)].append(layout.target)
    for (family, platform), targets in groups.items():
        if len(targets) < 2:
            continue
        common_paths = set.intersection(*(set(overlay_maps[target]) for target in targets))
        escaped = [
            rel for rel in sorted(common_paths)
            if len({digest(overlay_maps[target][rel]) for target in targets}) == 1
        ]
        if escaped:
            fail(
                f"{family}/{platform}: {len(escaped)} identical files remain in every target overlay; "
                f"move them to family/platform layer (first: {escaped[0]})"
            )

    validate_loader_boundaries(shared_root, family_roots, platform_roots)
    validate_preprocessor_contract()
    validate_all_directives(props)
    condition_files, condition_blocks = validate_condition_policy(
        [shared_root, *family_roots.values(), *platform_roots.values()],
        [layout.overlay_root for layout in layouts],
    )

    physical_files = (
        len(shared_map)
        + sum(len(values) for values in family_maps.values())
        + sum(len(values) for values in platform_maps.values())
        + sum(len(values) for values in overlay_maps.values())
    )
    effective_files = sum(len(layout.effective_files()) for layout in layouts)
    saved = effective_files - physical_files
    reduction = saved / effective_files * 100 if effective_files else 0.0

    print(
        "Hybrid source layout OK: "
        f"shared={len(shared_map)}, "
        + ", ".join(f"family/{name}={len(files)}" for name, files in family_maps.items())
        + ", "
        + ", ".join(f"platform/{name}={len(files)}" for name, files in platform_maps.items())
    )
    print(
        "Target overlays: "
        + ", ".join(f"{target}={len(files)}" for target, files in overlay_maps.items())
    )
    print(
        f"Inline version conditions: {condition_files} files / {condition_blocks} blocks; "
        f"physical files={physical_files}, effective files={effective_files}, "
        f"duplicate copies eliminated={saved} ({reduction:.1f}%)"
    )


if __name__ == "__main__":
    main()
