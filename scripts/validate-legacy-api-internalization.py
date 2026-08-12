#!/usr/bin/env python3
"""Validate API2 Stage 9: implementation-only legacy API helpers are internalized."""
from __future__ import annotations

import re
from pathlib import Path

from source_layout import load_properties, target_ids, target_layout

ROOT = Path(__file__).resolve().parents[1]
SOURCE_ROOTS = (
    "source-shared/src/main/java",
    "source-families/legacy/src/main/java",
    "source-families/modern/src/main/java",
    "source-platforms/forge/src/main/java",
    "source-platforms/neoforge/src/main/java",
    "version-src/1.19.2-forge/src/main/java",
    "version-src/1.20.1-forge/src/main/java",
    "version-src/1.21.1-neoforge/src/main/java",
    "source-shared/src/test/java",
    "source-platforms/forge/src/test/java",
    "source-platforms/neoforge/src/test/java",
    "addon-fixture/src/main/java",
)

LEGACY_DECL = re.compile(r"^\s*(?:package|import)\s+buildcraft\.api\.(?!v2(?:\.|;))", re.MULTILINE)

EXPECTED = (
    "source-shared/src/main/java/buildcraft/lib/internal/core/BlockIndex.java",
    "source-shared/src/main/java/buildcraft/lib/internal/core/EnumPipePart.java",
    "source-shared/src/main/java/buildcraft/lib/internal/core/IStackFilter.java",
    "source-shared/src/main/java/buildcraft/lib/internal/core/InvalidInputDataException.java",
    "source-shared/src/main/java/buildcraft/lib/internal/core/SafeTimeTracker.java",
    "source-shared/src/main/java/buildcraft/lib/internal/data/NbtSquishConstants.java",
    "source-shared/src/main/java/buildcraft/lib/internal/properties/BuildCraftProperties.java",
    "source-shared/src/main/java/buildcraft/lib/internal/recipes/StackDefinition.java",
    "source-families/legacy/src/main/java/buildcraft/lib/internal/core/render/ISprite.java",
    "source-families/modern/src/main/java/buildcraft/lib/internal/core/render/ISprite.java",
    "source-families/modern/src/main/java/buildcraft/lib/internal/inventory/IItemTransactor.java",
    "source-families/modern/src/main/java/buildcraft/lib/internal/recipes/IngredientStack.java",
    "source-platforms/forge/src/main/java/buildcraft/lib/internal/core/IFluidFilter.java",
    "source-platforms/forge/src/main/java/buildcraft/lib/internal/core/IFluidHandlerAdv.java",
    "source-platforms/forge/src/main/java/buildcraft/lib/internal/inventory/IItemHandlerFiltered.java",
    "source-platforms/forge/src/main/java/buildcraft/lib/internal/inventory/IItemTransactor.java",
    "source-platforms/forge/src/main/java/buildcraft/lib/internal/recipes/IngredientStack.java",
    "source-platforms/neoforge/src/main/java/buildcraft/lib/internal/capabilities/BCCapabilityRegistration.java",
    "source-platforms/neoforge/src/main/java/buildcraft/lib/internal/capabilities/IBCCapabilityProvider.java",
    "source-platforms/neoforge/src/main/java/buildcraft/lib/internal/core/IFluidFilter.java",
    "source-platforms/neoforge/src/main/java/buildcraft/lib/internal/core/IFluidHandlerAdv.java",
    "source-platforms/neoforge/src/main/java/buildcraft/lib/internal/inventory/IItemHandlerFiltered.java",
)


INTERNALIZED_TYPES = {
    "buildcraft.lib.internal.capabilities.BCCapabilityRegistration",
    "buildcraft.lib.internal.capabilities.IBCCapabilityProvider",
    "buildcraft.lib.internal.core.BlockIndex",
    "buildcraft.lib.internal.core.EnumPipePart",
    "buildcraft.lib.internal.core.IFluidFilter",
    "buildcraft.lib.internal.core.IFluidHandlerAdv",
    "buildcraft.lib.internal.core.IStackFilter",
    "buildcraft.lib.internal.core.InvalidInputDataException",
    "buildcraft.lib.internal.core.SafeTimeTracker",
    "buildcraft.lib.internal.core.render.ISprite",
    "buildcraft.lib.internal.data.NbtSquishConstants",
    "buildcraft.lib.internal.inventory.IItemHandlerFiltered",
    "buildcraft.lib.internal.inventory.IItemTransactor",
    "buildcraft.lib.internal.properties.BuildCraftProperties",
    "buildcraft.lib.internal.recipes.IngredientStack",
    "buildcraft.lib.internal.recipes.StackDefinition",
}
INTERNAL_IMPORT = re.compile(r"^\s*import\s+(buildcraft\.lib\.internal\.[^;]+);", re.MULTILINE)

PACKAGE_BY_FRAGMENT = {
    "/lib/internal/capabilities/": "buildcraft.lib.internal.capabilities",
    "/lib/internal/core/render/": "buildcraft.lib.internal.core.render",
    "/lib/internal/core/": "buildcraft.lib.internal.core",
    "/lib/internal/data/": "buildcraft.lib.internal.data",
    "/lib/internal/inventory/": "buildcraft.lib.internal.inventory",
    "/lib/internal/properties/": "buildcraft.lib.internal.properties",
    "/lib/internal/recipes/": "buildcraft.lib.internal.recipes",
}


def main() -> int:
    errors: list[str] = []
    legacy_files: list[str] = []

    for relative in SOURCE_ROOTS:
        root = ROOT / relative
        if not root.exists():
            continue
        for path in root.rglob("*.java"):
            text = path.read_text(encoding="utf-8", errors="ignore")
            rel = path.relative_to(ROOT).as_posix()
            if LEGACY_DECL.search(text):
                errors.append(f"{rel}: legacy buildcraft.api package/import remains")
            marker = "/buildcraft/api/"
            posix = path.as_posix()
            if marker in posix and "/buildcraft/api/v2/" not in posix:
                legacy_files.append(rel)

    if legacy_files:
        errors.append(f"legacy Java API source tree still contains {len(legacy_files)} file(s)")
        errors.extend(f"legacy source remains: {rel}" for rel in sorted(legacy_files))

    for rel in EXPECTED:
        path = ROOT / rel
        if not path.is_file():
            errors.append(f"missing internalized Stage 9 source: {rel}")
            continue
        text = path.read_text(encoding="utf-8", errors="ignore")
        expected_package = next(
            (pkg for fragment, pkg in PACKAGE_BY_FRAGMENT.items() if fragment in "/" + rel),
            None,
        )
        if expected_package and f"package {expected_package};" not in text:
            errors.append(f"{rel}: expected package {expected_package}")

    # These two package descriptors are internal too. Their historical license/provenance text is preserved.
    for rel in (
        "source-platforms/forge/src/main/java/buildcraft/lib/internal/recipes/package-info.java",
        "source-platforms/neoforge/src/main/java/buildcraft/lib/internal/recipes/package-info.java",
    ):
        path = ROOT / rel
        if not path.is_file():
            errors.append(f"missing internalized recipe package descriptor: {rel}")
        elif "package buildcraft.lib.internal.recipes;" not in path.read_text(encoding="utf-8", errors="ignore"):
            errors.append(f"{rel}: package descriptor did not move below buildcraft.lib.internal")

    # Every active target must resolve the internalized imports from its own effective source graph.
    properties = load_properties()
    for target in target_ids(properties):
        layout = target_layout(target, properties)
        effective = layout.effective_files()
        available = {
            rel[len("src/main/java/"):-5].replace("/", ".")
            for rel in effective
            if rel.startswith("src/main/java/") and rel.endswith(".java") and not rel.endswith("package-info.java")
        }
        for rel, source in effective.items():
            if not (rel.startswith("src/main/java/") and rel.endswith(".java")):
                continue
            text = source.read_text(encoding="utf-8", errors="ignore")
            for imported in INTERNAL_IMPORT.findall(text):
                outer = imported
                if outer.endswith(".IItemTransactor.IItemExtractable") or outer.endswith(".IItemTransactor.IItemInsertable"):
                    outer = outer.rsplit(".", 1)[0]
                if outer in INTERNALIZED_TYPES and outer not in available:
                    errors.append(f"{target}: {rel} imports missing internalized type {imported}")

    migration_map = (ROOT / "docs/api2/LEGACY_IMPORT_MIGRATION_MAP.csv").read_text(encoding="utf-8")
    if "Stage 9 complete:" not in migration_map:
        errors.append("legacy migration map does not record Stage 9 completion")

    if errors:
        print("Legacy API internalization FAILED:")
        for error in errors:
            print(f" - {error}")
        return 1

    print(
        "Legacy API internalization OK: 18 Stage 9 legacy symbols are implementation-only; "
        "0 non-v2 buildcraft.api imports/packages remain and the legacy Java API source tree is empty"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
