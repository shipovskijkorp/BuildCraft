#!/usr/bin/env python3
"""Validate the Gradle-8-compatible BuildCraft Stonecutter target matrix."""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

from source_layout import target_layout as layered_target_layout

ROOT = Path(__file__).resolve().parents[1]
SETTINGS = ROOT / "settings.gradle.kts"
ROOT_BUILD = ROOT / "stonecutter.gradle.kts"
TARGET_PROPERTIES = ROOT / "stonecutter-targets.properties"
WRAPPER_PROPERTIES = ROOT / "gradle/wrapper/gradle-wrapper.properties"
FORGE_BUILD = ROOT / "build.forge.gradle"
NEOFORGE_BUILD = ROOT / "build.neoforge.gradle"

ACTIVE_PATTERN = re.compile(r'^\s*stonecutter\s+active\s+"([^"]+)"', re.MULTILINE)
PLACEHOLDER_PATTERN = re.compile(r"\$\{([A-Za-z0-9_.-]+)}")
RESOURCE_KEY_PATTERN = re.compile(r'^\s*([A-Za-z_][A-Za-z0-9_]*)\s*:', re.MULTILINE)

COMMON_REQUIRED = (
    "mod.group",
    "mod.id",
    "mod.name",
    "mod.version",
    "mod.archive_name",
    "mod.authors",
    "mod.license",
    "deps.junit",
)

TARGET_REQUIRED = (
    "source.family",
    "source.shared_root",
    "source.root",
    "source.overlay_root",
    "deps.minecraft",
    "java.version",
    "network.protocol",
    "pack.format",
    "pack.resource_format",
    "pack.data_format",
)

FORGE_REQUIRED = (
    "deps.forge",
    "loader.version_range",
    "forge.version_range",
    "minecraft.version_range",
    "buildcraft.version_range",
    "compat.jei.range",
    "compat.jade.range",
    "compat.ic2.range",
    "compat.forestry.range",
)

NEOFORGE_REQUIRED = (
    "deps.neoforge",
    "loader.version_range",
    "neoforge.version_range",
    "minecraft.version_range",
    "buildcraft.version_range",
    "compat.jei.range",
    "compat.jade.range",
    "compat.ic2.range",
    "compat.forestry.range",
)


def fail(message: str) -> None:
    print(f"ERROR: {message}", file=sys.stderr)
    raise SystemExit(1)


def load_properties(path: Path) -> dict[str, str]:
    if not path.is_file():
        fail(f"missing configuration file: {path.relative_to(ROOT)}")

    result: dict[str, str] = {}
    pending = ""
    for line_number, raw_line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        line = pending + raw_line
        if line.endswith("\\") and not line.endswith("\\\\"):
            pending = line[:-1]
            continue
        pending = ""

        stripped = line.strip()
        if not stripped or stripped.startswith(("#", "!")):
            continue

        separator = line.find("=")
        if separator < 0:
            fail(f"{path.name}:{line_number}: expected key=value")
        key = line[:separator].strip()
        value = line[separator + 1 :].strip()
        if not key:
            fail(f"{path.name}:{line_number}: empty property key")
        if key in result:
            fail(f"{path.name}:{line_number}: duplicate property {key!r}")
        result[key] = value

    if pending:
        fail(f"{path.name}: dangling line continuation")
    return result


def required(properties: dict[str, str], key: str) -> str:
    value = properties.get(key, "").strip()
    if not value:
        fail(f"missing required property {key!r} in {TARGET_PROPERTIES.name}")
    return value


def target_value(properties: dict[str, str], target: str, key: str, *, allow_empty: bool = False) -> str:
    target_key = f"target.{target}.{key}"
    common_key = f"common.{key}"
    if target_key in properties:
        value = properties[target_key].strip()
    elif common_key in properties:
        value = properties[common_key].strip()
    else:
        fail(f"missing property {key!r} for target {target!r}")

    if not allow_empty and not value:
        fail(f"property {key!r} for target {target!r} must not be empty")
    return value


def parse_targets(properties: dict[str, str]) -> list[str]:
    targets = [item.strip() for item in required(properties, "targets").split(",") if item.strip()]
    if not targets:
        fail("target matrix is empty")
    if len(targets) != len(set(targets)):
        fail("target matrix contains duplicate target IDs")
    for target in targets:
        if "-" not in target or not target.rsplit("-", 1)[1]:
            fail(f"target {target!r} must end with a loader suffix")
    return targets


def resource_placeholders(loader: str, target: str, properties: dict[str, str]) -> set[str]:
    layout = layered_target_layout(target, properties)
    candidates: dict[str, tuple[str, ...]] = {
        "forge": (
            "src/main/resources/META-INF/mods.toml",
            "src/main/resources/pack.mcmeta",
        ),
        "neoforge": (
            "src/main/resources/META-INF/neoforge.mods.toml",
            "src/main/resources/pack.mcmeta",
        ),
        "fabric": (
            "src/main/resources/fabric.mod.json",
            "src/main/resources/pack.mcmeta",
        ),
    }
    placeholders: set[str] = set()
    for relative in candidates.get(loader, ()):
        path = layout.resolve(relative)
        if path is not None:
            placeholders.update(PLACEHOLDER_PATTERN.findall(path.read_text(encoding="utf-8")))
    return placeholders


def validate(properties: dict[str, str], targets: list[str]) -> tuple[str, str, int]:
    for legacy_name in ("settings.gradle", "build.gradle"):
        if (ROOT / legacy_name).exists():
            fail(f"legacy {legacy_name} shadows the Stonecutter Kotlin controller; remove it")

    settings_text = SETTINGS.read_text(encoding="utf-8")
    root_build_text = ROOT_BUILD.read_text(encoding="utf-8")
    wrapper_text = WRAPPER_PROPERTIES.read_text(encoding="utf-8")
    forge_build_text = FORGE_BUILD.read_text(encoding="utf-8")
    neoforge_build_text = NEOFORGE_BUILD.read_text(encoding="utf-8")

    if 'id("dev.kikugie.stonecutter") version "0.7.11"' not in settings_text:
        fail("Stonecutter must be pinned to the Gradle-8-compatible version 0.7.11")
    if "kotlinController = true" not in settings_text:
        fail("Stonecutter 0.7.11 requires kotlinController = true for stonecutter.gradle.kts")
    if "0.9.7" in settings_text or "StonecutterExperimentalAPI" in root_build_text:
        fail("Gradle-9-only Stonecutter 0.9 APIs remain in the project")
    if re.search(r"\bproperties\s*\{\s*tags\(", root_build_text):
        fail("Structured Properties is a Stonecutter 0.9 API and cannot be used on Gradle 8")
    if "stonecutter-targets.properties" not in settings_text:
        fail("settings must read stonecutter-targets.properties")
    if "version(targetId, minecraftVersion).buildscript" not in settings_text:
        fail("settings.gradle.kts does not register target-specific build scripts dynamically")

    if "gradle-8.8-bin.zip" not in wrapper_text:
        fail("Gradle wrapper must remain pinned to Gradle 8.8")
    if "https://maven.minecraftforge.net/" not in settings_text:
        fail("pluginManagement must include the MinecraftForge Maven repository")

    if "net.minecraftforge.accesstransformers" in root_build_text or "net.minecraftforge.accesstransformers" in forge_build_text:
        fail("legacy standalone AccessTransformers plugin is incompatible with Gradle 8+")
    if "id 'net.minecraftforge.gradle' version '[6.0,6.2)'" not in forge_build_text:
        fail("Forge targets must explicitly use ForgeGradle 6.x")
    if "net.minecraftforge.renamer" in root_build_text or "net.minecraftforge.renamer" in forge_build_text:
        fail("standalone Renamer must not be used with ForgeGradle 6")
    if "fg.deobf" not in forge_build_text:
        fail("ForgeGradle 6 build must use fg.deobf for mod dependencies")
    if "afterEvaluate {" not in forge_build_text \
            or "tasks.findByName('reobfJar')" not in forge_build_text \
            or "tasks.getByName('jar').finalizedBy(reobfTask)" not in forge_build_text:
        fail("reobfJar must be attached conditionally after project evaluation")
    if "tasks.matching { it.name == 'reobfJar' }.configureEach" in forge_build_text:
        fail("nested TaskProvider configuration breaks Gradle 8.8 task creation")
    if "finalizedBy 'reobfJar'" in forge_build_text:
        fail("unconditional reobfJar dependency breaks the Forge 1.21.1 target")
    if "stonecutter-targets.properties" not in forge_build_text:
        fail("Forge build must load the Gradle-8-compatible target configuration")
    if "resolveSourceFile('src/main/resources/META-INF/accesstransformer.cfg')" not in forge_build_text \
            or "accessTransformer = targetAccessTransformer" not in forge_build_text:
        fail("Forge build must resolve the access transformer from the layered source family")
    for token in ("sourceLayers", "layeredDirs", "prepareEffectiveSource"):
        if token not in forge_build_text:
            fail(f"Forge build is missing layered source support: {token}")
    if "resolveSourceFile('src/main/resources/META-INF/accesstransformer.cfg')" not in neoforge_build_text:
        fail("NeoForge build must resolve the access transformer from the layered source family")
    for token in ("sourceLayers", "layeredDirs", "prepareEffectiveSource"):
        if token not in neoforge_build_text:
            fail(f"NeoForge build is missing layered source support: {token}")

    active_match = ACTIVE_PATTERN.search(root_build_text)
    if not active_match:
        fail("stonecutter.gradle.kts has no active target declaration")
    active = active_match.group(1)
    if active not in targets:
        fail(f"active target {active!r} is not registered")

    vcs_target = required(properties, "vcsTarget")
    if vcs_target not in targets:
        fail(f"vcsTarget {vcs_target!r} is not registered")

    behavior_reference = required(properties, "behaviorReference")
    if behavior_reference != "1.19.2-forge":
        fail(f"behaviorReference must remain '1.19.2-forge', got {behavior_reference!r}")
    families = [item.strip() for item in required(properties, "sourceFamilies").split(",") if item.strip()]
    if families != ["legacy", "modern"]:
        fail(f"sourceFamilies must be legacy,modern; got {families}")

    if re.search(r"\bstonecutter\s+registerChiseled\b", root_build_text) \
            or "stonecutter.chiseled" in root_build_text:
        fail("Stonecutter 0.7.11 controller must not use the 0.8/0.9 chiseled task API")
    if 'tasks.register("buildAndCollect")' not in root_build_text:
        fail("root controller does not register buildAndCollect")
    if 'val targetsByFamily' not in root_build_text or 'tasks.register("build${family.replaceFirstChar' not in root_build_text:
        fail("root controller must expose per-family build tasks")
    if 'dependsOn(registeredTargets.map' not in root_build_text \
            or '":$target:buildAndCollect"' not in root_build_text:
        fail("root buildAndCollect must depend on every registered target task")

    for key in COMMON_REQUIRED:
        required(properties, f"common.{key}")

    placeholder_count = 0
    for target in targets:
        loader = target.rsplit("-", 1)[1]
        loader_required = FORGE_REQUIRED if loader == "forge" else NEOFORGE_REQUIRED if loader == "neoforge" else ()
        for key in TARGET_REQUIRED + loader_required:
            target_value(properties, target, key)

        if loader in {"forge", "neoforge"}:
            for compat in ("jei", "jade"):
                enabled = properties.get(
                    f"target.{target}.compat.{compat}.enabled", "true"
                ).strip().lower() != "false"
                dependency = target_value(properties, target, f"deps.{compat}", allow_empty=True)
                if enabled and not dependency:
                    fail(
                        f"target {target!r} enables {compat} compatibility but "
                        f"target.{target}.deps.{compat} is empty"
                    )

        configured_minecraft = target_value(properties, target, "deps.minecraft")
        if not target.startswith(configured_minecraft + "-"):
            fail(
                f"target {target!r} declares deps.minecraft={configured_minecraft!r}; "
                "the target ID must start with the Minecraft version"
            )

        layout = layered_target_layout(target, properties)
        if layout.family not in {"legacy", "modern"}:
            fail(f"target {target} has unknown source family {layout.family!r}")
        for layer_name, source_root in (
            ("shared", layout.shared_root),
            ("family", layout.family_root),
            ("overlay", layout.overlay_root),
        ):
            if not source_root.is_dir():
                fail(f"target {target} {layer_name} source root does not exist: {source_root}")
            if (source_root / ".git").exists():
                fail(f"target {target} {layer_name} source root must not contain a nested .git directory")

        source_maps = []
        for source_root in layout.layers:
            source_maps.append({
                path.relative_to(source_root).as_posix()
                for path in source_root.rglob("*") if path.is_file()
            })
        for index, left in enumerate(source_maps):
            for right in source_maps[index + 1:]:
                overlap = left & right
                if overlap:
                    fail(f"target {target} has files duplicated between source layers: {sorted(overlap)[0]}")

        build_script = ROOT / f"build.{loader}.gradle"
        if not build_script.is_file():
            fail(f"target {target} requires missing {build_script.name}")
        build_text = build_script.read_text(encoding="utf-8")
        if "buildAndCollect" not in build_text:
            fail(f"{build_script.name} does not define buildAndCollect")

        placeholders = resource_placeholders(loader, target, properties)
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

    return active, vcs_target, placeholder_count


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--list-targets", action="store_true", help="print one target ID per line")
    parser.add_argument("--loader", help="filter --list-targets by loader suffix")
    parser.add_argument("--family", choices=("legacy", "modern"), help="filter --list-targets by source family")
    args = parser.parse_args()

    properties = load_properties(TARGET_PROPERTIES)
    targets = parse_targets(properties)

    if args.list_targets:
        for target in targets:
            loader_matches = args.loader is None or target.rsplit("-", 1)[1] == args.loader
            family_matches = args.family is None or target_value(properties, target, "source.family") == args.family
            if loader_matches and family_matches:
                print(target)
        return

    active, vcs_target, placeholder_count = validate(properties, targets)
    print(
        "Stonecutter layout OK: "
        f"{len(targets)} target(s), active={active}, vcsTarget={vcs_target}, "
        f"families=legacy/modern, resource placeholders={placeholder_count}, Stonecutter=0.7.11, Gradle=8.8"
    )


if __name__ == "__main__":
    main()
