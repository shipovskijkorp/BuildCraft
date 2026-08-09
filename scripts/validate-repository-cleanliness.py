#!/usr/bin/env python3
"""Reject known porting debris and generated junk from maintained BCCE sources."""

from __future__ import annotations

from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
SOURCE_ROOTS = [ROOT / "source-shared", ROOT / "source-families", ROOT / "version-src"]

errors: list[str] = []

# Files that are never valid repository sources/artifacts.
for path in ROOT.rglob("*"):
    if ".git" in path.parts or "build" in path.parts or ".gradle" in path.parts:
        continue
    if path.is_dir() and path.name == "__pycache__":
        errors.append(f"generated Python cache directory is tracked/present: {path.relative_to(ROOT)}")
    elif path.is_file() and (
        path.name in {".DS_Store", "Thumbs.db"}
        or path.suffix in {".pyc", ".class", ".orig", ".rej", ".swp", ".tmp", ".bak"}
        or path.name.endswith("~")
    ):
        errors.append(f"generated/backup file is present: {path.relative_to(ROOT)}")

# Porting scratch classes used '$' prefixes in the old 1.19.3/1.20 transition.
for source_root in SOURCE_ROOTS:
    if not source_root.exists():
        continue
    for path in source_root.rglob("*.java"):
        if "$" in path.name:
            errors.append(f"temporary '$' Java source remains: {path.relative_to(ROOT)}")
        if path.name == "BCMenuBase_Neptune.java":
            errors.append(f"superseded menu base remains: {path.relative_to(ROOT)}")
        if path.name == "BCSiliconConfig.java":
            errors.append(f"unregistered silicon config stub remains: {path.relative_to(ROOT)}")
        if path.name == "ItemGoggles.java":
            errors.append(f"unregistered robot-goggles implementation remains: {path.relative_to(ROOT)}")


# Robot goggles were never registered in any supported target. Keep the dead
# class/assets/atlas references from returning as porting debris.
for source_root in SOURCE_ROOTS:
    if not source_root.exists():
        continue
    for path in source_root.rglob("*"):
        if not path.is_file():
            continue
        if path.name in {"goggles.json", "goggles.png"} and "buildcraftcore" in path.parts:
            errors.append(f"unregistered robot-goggles asset remains: {path.relative_to(ROOT)}")
        if path.suffix in {".json", ".java"}:
            text = path.read_text(encoding="utf-8", errors="replace")
            if "buildcraftcore:items/goggles" in text or "item.buildcraft.robot_goggles" in text:
                errors.append(f"unregistered robot-goggles reference remains: {path.relative_to(ROOT)}")

# Exact legacy lifecycle fragments that have no meaning on the supported ports.
for source_root in SOURCE_ROOTS:
    if not source_root.exists():
        continue
    for path in source_root.rglob("*.java"):
        text = path.read_text(encoding="utf-8", errors="replace")
        rel = path.relative_to(ROOT)
        for marker in (
            "@Mod.EventHandler",
            "NetworkRegistry.INSTANCE.registerGuiHandler",
            "FMLPreInitializationEvent",
            "FMLPostInitializationEvent",
            "player.openGui(",
        ):
            if marker in text:
                errors.append(f"obsolete Forge lifecycle marker {marker!r} remains in {rel}")
        if "TEMPORARY CLASS DO NOT USE" in text and str(rel).replace("\\", "/") != \
                "source-shared/src/main/java/buildcraft/api/recipes/StackDefinition.java":
            errors.append(f"temporary implementation marker remains in {rel}")


# Resource layout is generation-specific after 1.21. Keep old plural resource
# directories out of modern sources and new singular directories out of legacy.
def check_resource_generation(root: Path, family: str) -> None:
    if not root.exists():
        return
    legacy_only = {"advancements", "loot_tables", "recipes", "structures"}
    modern_only = {"advancement", "loot_table", "recipe", "structure"}
    legacy_tag_kinds = {"blocks", "items", "fluids"}
    modern_tag_kinds = {"block", "item", "fluid"}

    for path in root.rglob("*"):
        if not path.is_file():
            continue
        try:
            rel = path.relative_to(root)
        except ValueError:
            continue
        parts = rel.parts
        # Expected prefix: src/<source-set>/resources/data/<namespace>/...
        if len(parts) < 7 or parts[0] != "src" or parts[2] != "resources" or parts[3] != "data":
            continue
        data_parts = parts[5:]
        if not data_parts:
            continue
        top = data_parts[0]
        if family == "legacy" and top in modern_only:
            errors.append(f"modern resource directory {top!r} remains in legacy source: {path.relative_to(ROOT)}")
        if family == "modern" and top in legacy_only:
            errors.append(f"legacy resource directory {top!r} remains in modern source: {path.relative_to(ROOT)}")
        if len(data_parts) >= 2 and top == "tags":
            kind = data_parts[1]
            if family == "legacy" and kind in modern_tag_kinds:
                errors.append(f"modern tag directory {kind!r} remains in legacy source: {path.relative_to(ROOT)}")
            if family == "modern" and kind in legacy_tag_kinds:
                errors.append(f"legacy tag directory {kind!r} remains in modern source: {path.relative_to(ROOT)}")


check_resource_generation(ROOT / "source-families/legacy", "legacy")
check_resource_generation(ROOT / "version-src/1.19.2-forge", "legacy")
check_resource_generation(ROOT / "version-src/1.20.1-forge", "legacy")
check_resource_generation(ROOT / "source-families/modern", "modern")
check_resource_generation(ROOT / "version-src/1.21.1-forge", "modern")
check_resource_generation(ROOT / "version-src/1.21.1-neoforge", "modern")

# Mining tool tags belong to the minecraft namespace. Module-local copies from
# the early ports were ignored by vanilla and drifted independently.
for source_root in SOURCE_ROOTS:
    if not source_root.exists():
        continue
    for path in source_root.rglob("*.json"):
        rel_parts = path.relative_to(source_root).parts
        if "data" not in rel_parts:
            continue
        try:
            i = rel_parts.index("data")
        except ValueError:
            continue
        if len(rel_parts) <= i + 4:
            continue
        namespace = rel_parts[i + 1]
        if (
            namespace.startswith("buildcraft")
            and rel_parts[i + 2] == "tags"
            and rel_parts[i + 3] in {"block", "blocks"}
            and rel_parts[i + 4] == "mineable"
        ):
            errors.append(f"obsolete module-local mining tag remains: {path.relative_to(ROOT)}")
        if namespace == "buildcraftbuilder":
            errors.append(f"obsolete typo namespace 'buildcraftbuilder' remains: {path.relative_to(ROOT)}")

# These strings came from the abandoned 1.12/early-port era and are false today.
lang = ROOT / "source-shared/src/main/resources/assets/buildcraft/lang/en_us.json"
if lang.exists():
    text = lang.read_text(encoding="utf-8", errors="replace")
    for stale in (
        "Robots aren't coming to 1.12.2",
        "Not usable in survival",
        "Nothing can be charged with this!",
        "No recipes use this!",
    ):
        if stale in text:
            errors.append(f"stale historical tooltip remains in {lang.relative_to(ROOT)}: {stale!r}")

if errors:
    print("Repository cleanliness validation failed:")
    for error in errors:
        print(f" - {error}")
    sys.exit(1)

print("Repository cleanliness OK: no known porting scratch classes, obsolete lifecycle/UI shims, wrong-generation resource paths, stale module-local mining tags, stale 1.12 tooltips, or generated junk detected")
