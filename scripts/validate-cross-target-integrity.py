#!/usr/bin/env python3
"""Cross-target resource, metadata, build-hygiene and hotspot validation.

This catches classes of regressions that compile/parity-count checks miss:
case-sensitive atlas/model resources, target metadata drift, stale datagen output,
translation shadowing, stack-size parity, and copied target implementations that
silently lose fixes on one maintained Minecraft line.
"""
from __future__ import annotations

import json
import re
import sys
from collections import Counter
from pathlib import Path

from source_layout import ROOT, load_properties, preprocess_text, target_ids, target_layout


def fail(message: str) -> None:
    print(f"ERROR: {message}", file=sys.stderr)
    raise SystemExit(1)


def resource_map(target: str, props: dict[str, str]) -> dict[str, Path]:
    layout = target_layout(target, props)
    result: dict[str, Path] = {}
    prefix = "src/main/resources/"
    for rel, path in layout.effective_files("src/main/resources").items():
        if not rel.startswith(prefix):
            continue
        result[rel[len(prefix):]] = path
    return result


def effective_java(target: str, rel: str, props: dict[str, str]) -> str:
    layout = target_layout(target, props)
    logical = Path("src/main/java") / rel
    path = layout.resolve(logical)
    if path is None:
        fail(f"{target}: missing effective Java source {logical.as_posix()}")
    minecraft = props[f"target.{target}.deps.minecraft"]
    return preprocess_text(
        path.read_text(encoding="utf-8"),
        minecraft=minecraft,
        family=layout.family,
        platform=layout.platform,
    )


def validate_atlases_and_case(props: dict[str, str]) -> None:
    for target in target_ids(props):
        resources = resource_map(target, props)
        atlas_rel = "assets/minecraft/atlases/blocks.json"
        if atlas_rel not in resources:
            continue
        atlas = json.loads(resources[atlas_rel].read_text(encoding="utf-8"))
        lower = {rel.lower(): rel for rel in resources}
        for index, source in enumerate(atlas.get("sources", [])):
            if not isinstance(source, dict) or source.get("type") != "minecraft:single":
                continue
            resource = source.get("resource")
            if not isinstance(resource, str) or ":" not in resource:
                fail(f"{target}: atlas single source #{index} has invalid resource {resource!r}")
            if resource != resource.lower():
                fail(f"{target}: atlas resource is not lowercase: {resource}")
            namespace, path = resource.split(":", 1)
            if not namespace.startswith("buildcraft"):
                continue
            texture_rel = f"assets/{namespace}/textures/{path}.png"
            if texture_rel not in resources:
                ci = lower.get(texture_rel.lower())
                suffix = f" (case-only candidate: {ci})" if ci else ""
                fail(f"{target}: atlas resource does not exist exactly: {resource}{suffix}")
            sprite = source.get("sprite")
            if sprite is not None:
                if not isinstance(sprite, str) or sprite != sprite.lower():
                    fail(f"{target}: atlas sprite id is not lowercase: {sprite!r}")

        # Catch the same class of regression in item models: if an exact texture
        # path is absent but a case-insensitive BuildCraft path exists, fail.
        for rel, model_path in resources.items():
            if "/models/item/" not in rel or not rel.endswith(".json"):
                continue
            try:
                model = json.loads(model_path.read_text(encoding="utf-8"))
            except Exception as exc:
                fail(f"{target}: invalid item model {rel}: {exc}")
            textures = model.get("textures")
            if not isinstance(textures, dict):
                continue
            for value in textures.values():
                if not isinstance(value, str) or value.startswith("#") or ":" not in value:
                    continue
                namespace, path = value.split(":", 1)
                if not namespace.startswith("buildcraft"):
                    continue
                texture_rel = f"assets/{namespace}/textures/{path}.png"
                if texture_rel in resources:
                    continue
                ci = lower.get(texture_rel.lower())
                if ci:
                    fail(
                        f"{target}: case-sensitive item texture miss in {rel}: "
                        f"{value} -> actual {ci}"
                    )


def validate_stack_parity(props: dict[str, str]) -> None:
    builders_rel = "buildcraft/builders/BCBuildersItems.java"
    core_rel = "buildcraft/core/BCCoreItems.java"
    for target in target_ids(props):
        builders = effective_java(target, builders_rel, props)
        core = effective_java(target, core_rel, props)
        lines = {line.strip() for line in builders.splitlines()}
        blueprint = next((line for line in lines if 'register("blueprint"' in line), "")
        template = next((line for line in lines if 'register("template"' in line), "")
        schematic = next((line for line in lines if 'register("schematic_single"' in line), "")
        map_location = next((line.strip() for line in core.splitlines() if 'register("map_location"' in line), "")
        for name, line in (("blueprint", blueprint), ("template", template), ("map_location", map_location)):
            if not line:
                fail(f"{target}: cannot find {name} registration")
            if ".stacksTo(" in line:
                fail(f"{target}: {name} must keep vanilla stack size 64, got explicit stacksTo: {line}")
        if not schematic or ".stacksTo(1)" not in schematic:
            fail(f"{target}: schematic_single must stack to exactly 1: {schematic or '<missing>'}")


def validate_metadata(props: dict[str, str]) -> None:
    for target in target_ids(props):
        version = props[f"target.{target}.deps.minecraft"]
        parts = [int(x) for x in version.split(".")]
        if len(parts) != 3:
            fail(f"{target}: expected x.y.z Minecraft version, got {version}")
        upper = f"{parts[0]}.{parts[1]}.{parts[2] + 1}"
        expected = f"[{version},{upper})"
        actual = props.get(f"target.{target}.minecraft.version_range")
        if actual != expected:
            fail(f"{target}: Minecraft range must be exact patch line {expected}, got {actual}")


def validate_datagen_isolation() -> None:
    for rel in ("builds/legacy/build.forge.gradle", "builds/modern/build.neoforge.gradle"):
        text = (ROOT / rel).read_text(encoding="utf-8")
        if 'new File(repositoryRoot, "build/generated-resources/' in text:
            fail(f"{rel}: datagen output still lives outside the target build directory")
        if "sourceSets.main.resources.srcDir(generatedResources)" in text:
            fail(f"{rel}: stale datagen output is still part of production main resources")
        for token in (
            'layout.buildDirectory.dir("generated-resources/datagen")',
            "project.delete(generatedResources)",
            "generatedResources.mkdirs()",
        ):
            if token not in text:
                fail(f"{rel}: missing datagen hygiene guard {token!r}")


def validate_language_policy(props: dict[str, str]) -> None:
    # English has one repository-wide source of truth. Other locales may be
    # target-specific, but no effective target may define the same key twice.
    en_us_files: list[Path] = []
    for base in (ROOT / "source-shared", ROOT / "source-families", ROOT / "source-platforms", ROOT / "version-src"):
        en_us_files.extend(p for p in base.rglob("src/main/resources/assets/*/lang/en_us.json") if p.is_file())
    expected = ROOT / "source-shared/src/main/resources/assets/buildcraft/lang/en_us.json"
    unexpected = sorted(p.relative_to(ROOT).as_posix() for p in en_us_files if p != expected)
    if unexpected:
        fail(f"en_us must have one authoritative source; first extra: {unexpected[0]}")
    if not expected.is_file():
        fail("missing authoritative source-shared BuildCraft en_us.json")

    duplicates: list[str] = []
    def hook(pairs):
        seen = set()
        out = {}
        for key, value in pairs:
            if key in seen:
                duplicates.append(key)
            seen.add(key)
            out[key] = value
        return out
    json.loads(expected.read_text(encoding="utf-8"), object_pairs_hook=hook)
    if duplicates:
        fail(f"duplicate translation key inside en_us.json: {duplicates[0]}")

    for target in target_ids(props):
        resources = resource_map(target, props)
        locale_files: dict[str, list[tuple[str, Path]]] = {}
        for rel, path in resources.items():
            match = re.fullmatch(r"assets/[^/]+/lang/([^/]+\.json)", rel)
            if match:
                locale_files.setdefault(match.group(1), []).append((rel, path))
        for locale, files in locale_files.items():
            owners: dict[str, str] = {}
            for rel, path in sorted(files):
                data = json.loads(path.read_text(encoding="utf-8"))
                for key in data:
                    previous = owners.get(key)
                    if previous is not None:
                        fail(f"{target}: duplicate {locale} translation key {key!r} in {previous} and {rel}")
                    owners[key] = rel


def validate_guide_resources() -> None:
    names = {
        "cover.png", "icons.png", "left_page.png", "left_page_back.png",
        "left_page_first.png", "note.png", "right_page.png", "right_page_back.png",
        "right_page_last.png",
    }
    shared = ROOT / "source-shared/src/main/resources/assets/buildcraftlib/textures/gui/guide"
    missing = sorted(name for name in names if not (shared / name).is_file())
    if missing:
        fail(f"shared guide textures incomplete: {missing}")
    for base in (
        ROOT / "source-families/modern/src/main/resources/assets/buildcraftlib/textures/gui/guide",
        ROOT / "version-src/1.20.1-forge/src/main/resources/assets/buildcraftlib/textures/gui/guide",
    ):
        leftovers = [p.name for p in base.glob("*.png")] if base.is_dir() else []
        if leftovers:
            fail(f"redundant modern guide texture remains in {base.relative_to(ROOT)}: {leftovers[0]}")


def validate_quartz_sprite_paths() -> None:
    bad_patterns = (
        'new ResourceLocation("quartz_block_top")',
        'ResourceLocation.withDefaultNamespace("quartz_block_top")',
        'ResourceLocation.parse("quartz_block_top")',
    )
    for base in (ROOT / "source-shared", ROOT / "source-families", ROOT / "source-platforms", ROOT / "version-src"):
        for path in base.rglob("*.java"):
            text = path.read_text(encoding="utf-8")
            for bad in bad_patterns:
                if bad in text:
                    fail(f"wrong vanilla quartz sprite path remains in {path.relative_to(ROOT)}: {bad}")


def validate_promoted_cross_generation_files() -> None:
    manifest = ROOT / "build-config/promoted-cross-generation-files.txt"
    if not manifest.is_file():
        fail("missing promoted cross-generation file manifest")
    rels = [line.strip() for line in manifest.read_text(encoding="utf-8").splitlines() if line.strip()]
    if not rels:
        fail("promoted cross-generation file manifest is empty")
    for rel in rels:
        shared = ROOT / "source-shared/src/main" / rel
        old20 = ROOT / "version-src/1.20.1-forge/src/main" / rel
        old21 = ROOT / "source-families/modern/src/main" / rel
        if not shared.is_file():
            fail(f"promoted cross-generation source missing from shared: {rel}")
        if old20.exists() or old21.exists():
            fail(f"promoted source was copied back into target/family layer: {rel}")


def validate_hotspots(props: dict[str, str]) -> None:
    common = {
        "buildcraft/energy/client/gui/GuiEngineFE.java": (
            "RenderSystem.enableBlend();", "RenderSystem.defaultBlendFunc();", "OVERLAY.drawAt",
        ),
        "buildcraft/energy/client/gui/GuiDynamoMJ.java": (
            "RenderSystem.enableBlend();", "RenderSystem.defaultBlendFunc();", "OVERLAY.drawAt",
        ),
        "buildcraft/robotics/gui/GuiZonePlanner.java": ("argbToAbgr(colour)",),
        "buildcraft/robotics/zone/ZonePlannerMapChunk.java": (
            "Heightmap.Types.WORLD_SURFACE", "new MapColourData(current.posY, colour)",
        ),
        "buildcraft/transport/client/model/PipeBaseModelGenStandard.java": (
            "loadSpritesCache", "generateTranslucent", "getPipeModelColour",
        ),
        "buildcraft/energy/BCEnergySprites.java": (
            "FE_BACK_R", "FE_SIDE_R", "DYNAMO_MJ_GUI",
        ),
        "buildcraft/factory/tile/TileMiningWell.java": (
            "isStandaloneWaterBlock", "api2MachineTypeId",
        ),
    }
    for target in target_ids(props):
        for rel, tokens in common.items():
            text = effective_java(target, rel, props)
            for token in tokens:
                if token not in text:
                    fail(f"{target}: protected cross-target hotspot {rel} lost {token!r}")


def validate_ci_wiring() -> None:
    ci = (ROOT / ".github/workflows/ci.yml").read_text(encoding="utf-8")
    for token in (
        "python scripts/validate-fe-compat.py",
        "python scripts/validate-cross-target-integrity.py",
        "Run GameTests for every production target in generation",
        '":${target}:runGameTestServer"',
    ):
        if token not in ci:
            fail(f"CI is missing required validation/runtime coverage: {token}")


def main() -> None:
    props = load_properties()
    validate_atlases_and_case(props)
    validate_stack_parity(props)
    validate_metadata(props)
    validate_datagen_isolation()
    validate_language_policy(props)
    validate_guide_resources()
    validate_quartz_sprite_paths()
    validate_promoted_cross_generation_files()
    validate_hotspots(props)
    validate_ci_wiring()
    print("Cross-target integrity OK:")
    print(" - exact-case atlas/model resources verified")
    print(" - item stack and Minecraft metadata parity verified")
    print(" - datagen isolated from production resources")
    print(" - one authoritative en_us source; target locales have no duplicate keys")
    print(" - protected cross-target hotspots and CI GameTests guarded")


if __name__ == "__main__":
    main()
