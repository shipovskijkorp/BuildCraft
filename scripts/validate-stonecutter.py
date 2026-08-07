#!/usr/bin/env python3
"""Validate the Gradle-8-compatible BuildCraft Stonecutter target matrix."""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SETTINGS = ROOT / "settings.gradle.kts"
ROOT_BUILD = ROOT / "stonecutter.gradle.kts"
TARGET_PROPERTIES = ROOT / "stonecutter-targets.properties"
WRAPPER_PROPERTIES = ROOT / "gradle/wrapper/gradle-wrapper.properties"
FORGE_BUILD = ROOT / "build.forge.gradle"

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


def resource_placeholders(loader: str, source_root: Path) -> set[str]:
    candidates: dict[str, tuple[Path, ...]] = {
        "forge": (
            source_root / "src/main/resources/META-INF/mods.toml",
            source_root / "src/main/resources/pack.mcmeta",
        ),
        "neoforge": (
            source_root / "src/main/resources/META-INF/neoforge.mods.toml",
            source_root / "src/main/resources/pack.mcmeta",
        ),
        "fabric": (
            source_root / "src/main/resources/fabric.mod.json",
            source_root / "src/main/resources/pack.mcmeta",
        ),
    }
    placeholders: set[str] = set()
    for path in candidates.get(loader, ()):
        if path.is_file():
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
    if "targetAccessTransformer = new File(targetSourceRoot" not in forge_build_text \
            or "accessTransformer = targetAccessTransformer" not in forge_build_text:
        fail("Forge build must configure the target-specific access transformer")

    active_match = ACTIVE_PATTERN.search(root_build_text)
    if not active_match:
        fail("stonecutter.gradle.kts has no active target declaration")
    active = active_match.group(1)
    if active not in targets:
        fail(f"active target {active!r} is not registered")

    vcs_target = required(properties, "vcsTarget")
    if vcs_target not in targets:
        fail(f"vcsTarget {vcs_target!r} is not registered")

    if re.search(r"\bstonecutter\s+registerChiseled\b", root_build_text) \
            or "stonecutter.chiseled" in root_build_text:
        fail("Stonecutter 0.7.11 controller must not use the 0.8/0.9 chiseled task API")
    if 'tasks.register("buildAndCollect")' not in root_build_text:
        fail("root controller does not register buildAndCollect")
    if 'dependsOn(registeredTargets.map' not in root_build_text \
            or '":$target:buildAndCollect"' not in root_build_text:
        fail("root buildAndCollect must depend on every registered target task")

    for key in COMMON_REQUIRED:
        required(properties, f"common.{key}")

    placeholder_count = 0
    for target in targets:
        loader = target.rsplit("-", 1)[1]
        for key in TARGET_REQUIRED + (FORGE_REQUIRED if loader == "forge" else ()):
            target_value(properties, target, key)

        if loader == "forge":
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

        source_root_value = properties.get(f"target.{target}.source.root", "").strip()
        source_root = ROOT / source_root_value if source_root_value else ROOT
        if not source_root.is_dir():
            fail(f"target {target} source root does not exist: {source_root}")
        for required_source_path in ("src/main/java", "src/main/resources"):
            if not (source_root / required_source_path).is_dir():
                fail(f"target {target} source root is missing {required_source_path}")
        if source_root != ROOT and (source_root / ".git").exists():
            fail(f"target {target} source root must not contain a nested .git directory")

        assets_root = source_root / "src/main/resources/assets"
        non_english_localizations = sorted(
            path.relative_to(source_root).as_posix()
            for path in assets_root.rglob("*.json")
            if path.parent.name == "lang" and path.name != "en_us.json"
        )
        non_english_guide_text = sorted(
            path.relative_to(source_root).as_posix()
            for path in assets_root.rglob("*.json")
            if path.parent.name == "text"
            and path.parent.parent.name == "guide"
            and path.name != "en_us.json"
        )
        bundled_non_english = non_english_localizations + non_english_guide_text
        if bundled_non_english:
            fail(
                f"target {target} bundles non-English translations that belong in "
                "BuildCraft Community Edition: Localizations: "
                + ", ".join(bundled_non_english[:12])
            )

        build_script = ROOT / f"build.{loader}.gradle"
        if not build_script.is_file():
            fail(f"target {target} requires missing {build_script.name}")
        build_text = build_script.read_text(encoding="utf-8")
        if "buildAndCollect" not in build_text:
            fail(f"{build_script.name} does not define buildAndCollect")

        placeholders = resource_placeholders(loader, source_root)
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
    args = parser.parse_args()

    properties = load_properties(TARGET_PROPERTIES)
    targets = parse_targets(properties)

    if args.list_targets:
        for target in targets:
            if args.loader is None or target.rsplit("-", 1)[1] == args.loader:
                print(target)
        return

    active, vcs_target, placeholder_count = validate(properties, targets)
    print(
        "Stonecutter layout OK: "
        f"{len(targets)} target(s), active={active}, vcsTarget={vcs_target}, "
        f"resource placeholders={placeholder_count}, Stonecutter=0.7.11, Gradle=8.8"
    )


if __name__ == "__main__":
    main()
