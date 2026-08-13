#!/usr/bin/env python3
"""Cross-target resource, metadata, build-hygiene and hotspot validation.

This catches classes of regressions that compile/parity-count checks miss:
case-sensitive atlas/model resources, target metadata drift, stale datagen output,
translation ownership, stack-size parity, pipe recipe schemas, transfer-profile wiring,
and copied target implementations that silently lose fixes on one maintained Minecraft line.
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
    # The main mod owns English only. Every other locale belongs to the separate
    # localization addon and must never leak back into production source layers.
    en_us_files: list[Path] = []
    for base in (ROOT / "source-shared", ROOT / "source-families", ROOT / "source-platforms", ROOT / "version-src"):
        en_us_files.extend(p for p in base.rglob("src/main/resources/assets/*/lang/en_us.json") if p.is_file())
    expected = ROOT / "source-shared/src/main/resources/assets/buildcraft/lang/en_us.json"
    unexpected = sorted(p.relative_to(ROOT).as_posix() for p in en_us_files if p != expected)
    if unexpected:
        fail(f"en_us must have one authoritative source; first extra: {unexpected[0]}")
    if not expected.is_file():
        fail("missing authoritative source-shared BuildCraft en_us.json")

    for base in (ROOT / "source-shared", ROOT / "source-families", ROOT / "source-platforms", ROOT / "version-src"):
        for path in base.rglob("src/main/resources/assets/*/lang/*.json"):
            if path.is_file() and path.name != "en_us.json":
                fail(f"main mod must contain en_us only; addon-owned locale remains: {path.relative_to(ROOT)}")
        for path in base.rglob("src/main/resources/assets/*/guide/text/*.json"):
            if path.is_file() and path.name != "en_us.json":
                fail(f"main mod guide must contain en_us only; addon-owned locale remains: {path.relative_to(ROOT)}")

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



def validate_pipe_recipe_schema(props: dict[str, str]) -> None:
    """Custom PipeRecipe uses result.item on every maintained target.

    Minecraft 1.21's codec errors are easy to miss during static JSON counting because
    the JSON remains syntactically valid. Validate the serializer contract explicitly.
    """
    for target in target_ids(props):
        resources = resource_map(target, props)
        for rel, path in resources.items():
            if not rel.startswith("data/buildcrafttransport/") or not rel.endswith(".json"):
                continue
            if "/recipe/" not in rel and "/recipes/" not in rel:
                continue
            try:
                data = json.loads(path.read_text(encoding="utf-8"))
            except Exception as exc:
                fail(f"{target}: invalid recipe JSON {rel}: {exc}")
            if data.get("type") != "buildcrafttransport:pipe":
                continue
            result = data.get("result")
            if not isinstance(result, dict):
                fail(f"{target}: custom pipe recipe {rel} has no object result")
            if "item" not in result:
                fail(f"{target}: custom pipe recipe {rel} must encode result.item, got {result}")
            if "id" in result:
                fail(f"{target}: custom pipe recipe {rel} still uses unsupported result.id")


def validate_pipe_transfer_wiring() -> None:
    bridge_rel = "source-shared/src/main/java/buildcraft/transport/api2/PipeTypeBridge.java"
    bridge = (ROOT / bridge_rel).read_text(encoding="utf-8")
    for token in (
        'DEFAULT_FLUID_BASE_RATE = 10',
        'DEFAULT_MJ_BASE_RATE = 4',
        'DEFAULT_FE_BASE_RATE = 40',
        'case "wood_fluid", "cobblestone_fluid"',
        'case "wood_power" -> { multiplier = 4; resistanceDivisor = 128; extractor = true; }',
        'case "wood_fe" -> { multiplier = 4; extractor = true; }',
    ):
        if token not in bridge:
            fail(f"{bridge_rel}: built-in API2 defaults lost {token!r}")

    for platform in ("forge", "neoforge"):
        base = ROOT / f"source-platforms/{platform}/src/main/java/buildcraft/transport"
        api_rel = base / "internal/pipe/PipeApi.java"
        api = api_rel.read_text(encoding="utf-8")
        for token in (
            "fluidTransferData.get(def)", "def.getApiType().fluidProfile()",
            "powerTransferData.get(def)", "def.getApiType().mjProfile()",
            "forgeEnergyTransferData.get(def)", "def.getApiType().externalEnergyProfile()",
        ):
            if token not in api:
                fail(f"{api_rel.relative_to(ROOT)}: config/API2 fallback wiring lost {token!r}")

        flow_expectations = {
            "pipe/flow/PipeFlowFluids.java": "PipeApi.getFluidTransferInfo(pipe.getDefinition())",
            "pipe/flow/PipeFlowPower.java": "PipeApi.getPowerTransferInfo(pipe.getDefinition())",
            "pipe/flow/PipeFlowForgeEnergy.java": "PipeApi.getForgeEnergyTransferInfo(pipe.getDefinition())",
        }
        forbidden = {
            "pipe/flow/PipeFlowFluids.java": "getApiType().fluidProfile()",
            "pipe/flow/PipeFlowPower.java": "getApiType().mjProfile()",
            "pipe/flow/PipeFlowForgeEnergy.java": "getApiType().externalEnergyProfile()",
        }
        for rel, token in flow_expectations.items():
            path = base / rel
            text = path.read_text(encoding="utf-8")
            if token not in text:
                fail(f"{path.relative_to(ROOT)}: runtime flow bypasses configured PipeApi transfer data")
            if forbidden[rel] in text:
                fail(f"{path.relative_to(ROOT)}: runtime flow still snapshots API2 profile instead of config override")



def validate_randomium_facade_blacklist(props: dict[str, str]) -> None:
    """Keep facade variants out of Randomium's creative-tab loot scan.

    Randomium 1.19/1.20 uses randomium:blacklist under the legacy item-tag
    directory, while its 1.21 line renamed the tag to randomium:randomium_blacklist
    and Minecraft 1.21 uses the singular item tag directory. The facade item emits
    many creative-tab variants, so losing this compatibility tag can turn
    Randomium's per-item linear de-duplication into a pathological startup cost.
    """
    expected = {
        "1.19.2-forge": "data/randomium/tags/items/blacklist.json",
        "1.20.1-forge": "data/randomium/tags/items/blacklist.json",
        "1.21.1-neoforge": "data/randomium/tags/item/randomium_blacklist.json",
    }
    facade_id = "buildcraftsilicon:plug/facade"
    for target, rel in expected.items():
        resources = resource_map(target, props)
        path = resources.get(rel)
        if path is None:
            fail(f"{target}: missing Randomium facade blacklist tag {rel}")
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except Exception as exc:
            fail(f"{target}: invalid Randomium facade blacklist JSON {rel}: {exc}")
        if data.get("replace") is not False:
            fail(f"{target}: Randomium facade blacklist must merge with Randomium defaults")
        values = data.get("values")
        if not isinstance(values, list) or facade_id not in values:
            fail(f"{target}: Randomium facade blacklist does not contain {facade_id}")


def validate_item_pipe_gametest_isolation() -> None:
    """Item-pipe conservation tests must only count their own marked cargo.

    Forge 1.19.2 runs all GameTests in a shared world and can expose unrelated
    ItemEntity instances inside a broad manually constructed AABB. A conservation
    test must therefore track the exact stack it injected, while still failing if
    that marked stack is actually duplicated/ejected by the pipe runtime.
    """
    for platform in ("forge", "neoforge"):
        path = ROOT / f"source-platforms/{platform}/src/gametest/java/buildcraft/gametest/BuildCraftPipeTransportGameTests.java"
        text = path.read_text(encoding="utf-8")
        for forbidden in ("countDroppedItems(", "new BlockPos(7, 3, 7)"):
            if forbidden in text:
                fail(f"{path.relative_to(ROOT)}: broad unscoped item-entity assertion returned: {forbidden!r}")
        for token in (
            'CARGO_MARKER_KEY = "buildcraft_test"',
            'TEST_BOUNDS_MAX = new BlockPos(6, 2, 6)',
            'countDroppedCargo(helper, "straight_line")',
            'countDroppedCargo(helper, "diamond_fallback")',
            'countDroppedCargo(helper, "accepted_count")',
            'countDroppedCargo(helper, "clay_preference")',
            '.filter(entity -> hasCargoMarker(entity.getItem(), marker))',
        ):
            if token not in text:
                fail(f"{path.relative_to(ROOT)}: cargo-isolated GameTest guard lost {token!r}")


def validate_ci_wiring() -> None:
    ci = (ROOT / ".github/workflows/ci.yml").read_text(encoding="utf-8")
    for token in (
        "python scripts/validate-fe-compat.py",
        "python scripts/validate-cross-target-integrity.py",
        "Run GameTests for every production target in generation",
        '":${target}:runGameTestServer"',
        "gametest_status=0",
        "if ! ./gradlew --no-daemon --console=plain --stacktrace",
        'exit "$gametest_status"',
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
    validate_pipe_recipe_schema(props)
    validate_pipe_transfer_wiring()
    validate_randomium_facade_blacklist(props)
    validate_item_pipe_gametest_isolation()
    validate_ci_wiring()
    print("Cross-target integrity OK:")
    print(" - exact-case atlas/model resources verified")
    print(" - item stack and Minecraft metadata parity verified")
    print(" - datagen isolated from production resources")
    print(" - main resources are en_us-only; localization addon ownership guarded")
    print(" - custom pipe recipe schema and configured transfer wiring verified")
    print(" - Randomium facade blacklist compatibility guarded on maintained targets")
    print(" - item-pipe GameTests track only their own marked cargo")
    print(" - protected cross-target hotspots and CI GameTests guarded")


if __name__ == "__main__":
    main()
