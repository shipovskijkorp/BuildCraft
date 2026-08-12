#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
SOURCE_ROOTS = [
    ROOT / "source-shared/src/main/java",
    ROOT / "source-families/legacy/src/main/java",
    ROOT / "source-families/modern/src/main/java",
    ROOT / "source-platforms/forge/src/main/java",
    ROOT / "source-platforms/neoforge/src/main/java",
    ROOT / "version-src/1.19.2-forge/src/main/java",
    ROOT / "version-src/1.20.1-forge/src/main/java",
    ROOT / "version-src/1.21.1-neoforge/src/main/java",
]

RETIRED = {
    "buildcraft.api.BCModules",
    "buildcraft.api.IBuildCraftMod",
    "buildcraft.api.blocks.CustomPaintHelper",
    "buildcraft.api.blocks.CustomRotationHelper",
    "buildcraft.api.blocks.ICustomPaintHandler",
    "buildcraft.api.blocks.ICustomRotationHandler",
    "buildcraft.api.core.BCDebugging",
    "buildcraft.api.core.BCLog",
    "buildcraft.api.core.BuildCraftAPI",
    "buildcraft.api.core.IAreaProvider",
    "buildcraft.api.core.IBox",
    "buildcraft.api.core.IConvertable",
    "buildcraft.api.core.IEngineType",
    "buildcraft.api.core.IPathProvider",
    "buildcraft.api.core.IPlayerOwned",
    "buildcraft.api.core.IZone",
    "buildcraft.api.enums.EnumDecoratedBlock",
    "buildcraft.api.enums.EnumEngineType",
    "buildcraft.api.enums.EnumLaserTableType",
    "buildcraft.api.enums.EnumMachineState",
    "buildcraft.api.enums.EnumOptionalSnapshotType",
    "buildcraft.api.enums.EnumPowerStage",
    "buildcraft.api.enums.EnumRedstoneChipset",
    "buildcraft.api.enums.EnumSnapshotType",
    "buildcraft.api.enums.EnumSpring",
    "buildcraft.api.tiles.IControllable",
    "buildcraft.api.tiles.IDebuggable",
    "buildcraft.api.tiles.IHasWork",
    "buildcraft.api.tiles.IHeatable",
    "buildcraft.api.tiles.ITileAreaProvider",
    "buildcraft.api.tiles.TilesAPI",
    "buildcraft.api.tools.IToolWrench",
}

IMPORT = re.compile(r"^\s*import\s+(buildcraft\.api\.(?!v2\.)[^;]+);", re.MULTILINE)


def read(rel: str) -> str:
    p = ROOT / rel
    if not p.is_file():
        raise AssertionError(f"missing {rel}")
    return p.read_text(encoding="utf-8", errors="ignore")


def main() -> int:
    errors: list[str] = []
    current: set[str] = set()

    for source_root in SOURCE_ROOTS:
        if not source_root.exists():
            continue
        for path in source_root.rglob("*.java"):
            text = path.read_text(encoding="utf-8", errors="ignore")
            rel = path.relative_to(ROOT)
            current.update(IMPORT.findall(text))
            for retired in RETIRED:
                if re.search(rf"\b(?:package|import)\s+{re.escape(retired)}(?:\s*;|\.)", text):
                    errors.append(f"{rel}: retired Stage 7 public symbol remains: {retired}")

    stale = sorted(RETIRED & current)
    if stale:
        errors.extend(f"retired Stage 7 import still active: {symbol}" for symbol in stale)

    runtime = read("source-shared/src/main/java/buildcraft/lib/internal/api/v2/BuildCraftApiRuntime.java")
    for service in ("ACTORS", "MODULES", "WRENCHES", "BLOCK_INTERACTIONS", "DEBUG_VIEWS"):
        if f"BuildCraftServices.{service}" not in runtime:
            errors.append(f"BuildCraftApiRuntime does not install {service}")

    for rel, tokens in {
        "source-shared/src/main/java/buildcraft/lib/internal/area/IAreaProvider.java": ("extends AreaProvider", "new BlockBox"),
        "source-shared/src/main/java/buildcraft/lib/internal/area/IPathProvider.java": ("extends AreaProvider", "Optional<Path>"),
        "source-shared/src/main/java/buildcraft/lib/internal/area/IZone.java": ("extends Zone", "contains(BlockPos"),
        "source-shared/src/main/java/buildcraft/lib/internal/permission/IPlayerOwned.java": ("extends OwnedView", "ownerId()"),
        "source-shared/src/main/java/buildcraft/lib/misc/WrenchUtil.java": ("BuildCraftServices.WRENCHES",),
        "source-shared/src/main/java/buildcraft/lib/internal/api/v2/BlockInteractionRuntime.java": (
            "BuildCraftServices.BLOCK_INTERACTIONS", "BuildCraftServices.ACTORS", "OperationMode.EXECUTE"
        ),
        "source-shared/src/main/java/buildcraft/lib/internal/api/v2/DebugServiceImpl.java": (
            "BuildCraftRegistries.DEBUG_CONTRIBUTORS", "instanceof IDebuggable"
        ),
        "source-shared/src/main/java/buildcraft/lib/internal/api/v2/ModuleServiceImpl.java": (
            "implements ModuleService", "BCModules.VALUES"
        ),
        "addon-fixture/src/main/java/dev/bcce/apifixture/ApiV2FixtureAddon.java": (
            "BuildCraftServices.MODULES", "BuildCraftServices.WRENCHES", "BuildCraftServices.DEBUG_VIEWS",
            "BuildCraftServices.BLOCK_INTERACTIONS", "BuildCraftRegistries.ROTATION_HANDLERS", "BuildCraftRegistries.PAINT_HANDLERS"
        ),
    }.items():
        data = read(rel)
        for token in tokens:
            if token not in data:
                errors.append(f"{rel}: missing Stage 7 API2 hook {token}")

    modules = read("source-shared/src/main/java/buildcraft/api/v2/module/BuildCraftModules.java")
    if "COMPAT" not in modules:
        errors.append("BuildCraftModules.COMPAT missing")

    legacy_count = len(current)
    if legacy_count != 34:
        errors.append(f"expected 34 remaining Stage 8/9 legacy imports after Stage 7, found {legacy_count}")

    if errors:
        print("Core / misc API2 migration FAILED:")
        for error in errors:
            print(f" - {error}")
        return 1

    print(
        "Core / misc API2 migration OK: 32 Stage 7 public symbols retired; "
        "area/ownership bridges and module/wrench/block/debug services are live; "
        f"{legacy_count} legacy imports remain for Stages 8-9"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
