#!/usr/bin/env python3
"""Validate the declarative Stonecutter target matrix without running Gradle."""

from __future__ import annotations

import re
import sys
import tomllib
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
SETTINGS = ROOT / "settings.gradle.kts"
ROOT_BUILD = ROOT / "stonecutter.gradle.kts"
PROPERTIES = ROOT / "stonecutter.properties.toml"

TARGET_PATTERN = re.compile(r'^\s*target\("([^"]+)"\s*,\s*(.+?)\)\s*$', re.MULTILINE)
QUOTED_PATTERN = re.compile(r'"([^"]+)"')
ACTIVE_PATTERN = re.compile(r'^\s*stonecutter\s+active\s+"([^"]+)"', re.MULTILINE)
VCS_PATTERN = re.compile(r'^\s*vcsVersion\s*=\s*"([^"]+)"', re.MULTILINE)
PLACEHOLDER_PATTERN = re.compile(r"\$\{([A-Za-z0-9_.-]+)}")
RESOURCE_KEY_PATTERN = re.compile(r'^\s*([A-Za-z_][A-Za-z0-9_]*)\s*:', re.MULTILINE)

COMMON_TARGET_PATHS = (
    "deps.minecraft",
    "java.version",
    "network.protocol",
    "pack.format",
    "pack.resource_format",
    "pack.data_format",
)

FORGE_TARGET_PATHS = (
    "deps.forge",
    "loader.version_range",
    "forge.version_range",
    "minecraft.version_range",
    "buildcraft.version_range",
)


def fail(message: str) -> None:
    print(f"ERROR: {message}", file=sys.stderr)
    raise SystemExit(1)


def nested_get(table: dict[str, Any], dotted_path: str) -> Any:
    value: Any = table
    for part in dotted_path.split("."):
        if not isinstance(value, dict) or part not in value:
            raise KeyError(dotted_path)
        value = value[part]
    return value


def parse_targets(settings_text: str) -> list[tuple[str, str]]:
    targets: list[tuple[str, str]] = []
    for match in TARGET_PATTERN.finditer(settings_text):
        minecraft_version = match.group(1)
        loaders = QUOTED_PATTERN.findall(match.group(2))
        if not loaders:
            fail(f'target("{minecraft_version}", ...) has no loaders')
        targets.extend((minecraft_version, loader) for loader in loaders)
    if not targets:
        fail("settings.gradle.kts does not register any Stonecutter targets")
    if len(targets) != len(set(targets)):
        fail("settings.gradle.kts contains duplicate version/loader targets")
    return targets


def resource_placeholders(loader: str) -> set[str]:
    # Metadata differs by loader. Validate only the files owned by the current
    # loader so adding Fabric/NeoForge does not require imitating Forge's map.
    candidates: dict[str, tuple[Path, ...]] = {
        "forge": (
            ROOT / "src/main/resources/META-INF/mods.toml",
            ROOT / "src/main/resources/pack.mcmeta",
        ),
        "neoforge": (
            ROOT / "src/main/resources/META-INF/neoforge.mods.toml",
            ROOT / "src/main/resources/pack.mcmeta",
        ),
        "fabric": (
            ROOT / "src/main/resources/fabric.mod.json",
            ROOT / "src/main/resources/pack.mcmeta",
        ),
    }
    placeholders: set[str] = set()
    for path in candidates.get(loader, ()):
        if path.is_file():
            placeholders.update(PLACEHOLDER_PATTERN.findall(path.read_text(encoding="utf-8")))
    return placeholders


def main() -> None:
    settings_text = SETTINGS.read_text(encoding="utf-8")
    root_build_text = ROOT_BUILD.read_text(encoding="utf-8")
    targets = parse_targets(settings_text)
    target_ids = {f"{version}-{loader}" for version, loader in targets}

    active_match = ACTIVE_PATTERN.search(root_build_text)
    if not active_match:
        fail("stonecutter.gradle.kts has no active target declaration")
    active = active_match.group(1)
    if active not in target_ids:
        fail(f"active target {active!r} is not registered in settings.gradle.kts")

    vcs_match = VCS_PATTERN.search(settings_text)
    if not vcs_match:
        fail("settings.gradle.kts has no vcsVersion")
    vcs_target = vcs_match.group(1)
    if vcs_target not in target_ids:
        fail(f"vcsVersion {vcs_target!r} is not a registered target")

    if 'tasks.register("buildAndCollect", stonecutter.chiseled)' not in root_build_text \
            or 'ofTask("buildAndCollect")' not in root_build_text:
        fail("root build does not register the chiseled buildAndCollect task")

    with PROPERTIES.open("rb") as handle:
        properties = tomllib.load(handle)

    for shared_path in ("mod.group", "mod.id", "mod.name", "mod.version", "mod.archive_name", "mod.authors", "mod.license"):
        try:
            nested_get(properties, shared_path)
        except KeyError:
            fail(f"missing shared Stonecutter property: {shared_path}")

    placeholder_count = 0
    for minecraft_version, loader in targets:
        target_id = f"{minecraft_version}-{loader}"
        loader_table = properties.get(loader)
        if not isinstance(loader_table, dict):
            fail(f"missing [{loader!r}] table for {target_id}")
        target_table = loader_table.get(minecraft_version)
        if not isinstance(target_table, dict):
            fail(f'missing [{loader}."{minecraft_version}"] table for {target_id}')

        for path in COMMON_TARGET_PATHS + (FORGE_TARGET_PATHS if loader == "forge" else ()):
            try:
                nested_get(target_table, path)
            except KeyError:
                fail(f"missing {path!r} in target table for {target_id}")

        configured_minecraft = str(nested_get(target_table, "deps.minecraft"))
        if configured_minecraft != minecraft_version:
            fail(
                f"target {target_id} declares deps.minecraft={configured_minecraft!r}; "
                f"expected {minecraft_version!r}"
            )

        build_script = ROOT / f"build.{loader}.gradle"
        if not build_script.is_file():
            fail(f"target {target_id} requires missing {build_script.name}")
        build_text = build_script.read_text(encoding="utf-8")
        if "buildAndCollect" not in build_text:
            fail(f"{build_script.name} does not define buildAndCollect")

        placeholders = resource_placeholders(loader)
        placeholder_count += len(placeholders)
        resource_block = re.search(
            r"def\s+resourceProperties\s*=\s*\[(.*?)\n\]",
            build_text,
            flags=re.DOTALL,
        )
        if placeholders and not resource_block:
            fail(f"{build_script.name} has no resourceProperties map")
        provided_keys = set(RESOURCE_KEY_PATTERN.findall(resource_block.group(1))) if resource_block else set()
        missing_placeholders = placeholders - provided_keys
        if missing_placeholders:
            fail(
                f"{build_script.name} does not provide resource placeholders: "
                + ", ".join(sorted(missing_placeholders))
            )

    print(
        "Stonecutter layout OK: "
        f"{len(targets)} target(s), active={active}, vcsVersion={vcs_target}, "
        f"resource placeholders={placeholder_count}"
    )


if __name__ == "__main__":
    main()
