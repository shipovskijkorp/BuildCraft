#!/usr/bin/env python3
"""Offline validation for the BuildCraft 8.0.12 Forge 1.21.1 beta port."""
from __future__ import annotations

import argparse
import json
import re
import sys
import tomllib
from pathlib import Path


def fail(message: str) -> None:
    print(f"ERROR: {message}", file=sys.stderr)
    raise SystemExit(1)


def load_properties(path: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            fail(f"invalid property line in {path}: {raw!r}")
        key, value = line.split("=", 1)
        result[key.strip()] = value.strip()
    return result


def scan_java(path: Path) -> None:
    text = path.read_text(encoding="utf-8", errors="strict")
    state = "normal"
    escaped = False
    line = 1
    stack: list[tuple[str, int]] = []
    pairs = {")": "(", "]": "[", "}": "{"}
    i = 0
    while i < len(text):
        char = text[i]
        nxt = text[i + 1] if i + 1 < len(text) else ""
        tri = text[i:i + 3]
        if char == "\n":
            line += 1
            if state == "line_comment":
                state = "normal"
            i += 1
            continue
        if state == "line_comment":
            i += 1
            continue
        if state == "block_comment":
            if char == "*" and nxt == "/":
                state = "normal"
                i += 2
            else:
                i += 1
            continue
        if state == "text_block":
            if tri == '"""':
                state = "normal"
                i += 3
            else:
                i += 1
            continue
        if state in {"string", "char"}:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif (state == "string" and char == '"') or (state == "char" and char == "'"):
                state = "normal"
            i += 1
            continue

        if char == "/" and nxt == "/":
            state = "line_comment"
            i += 2
        elif char == "/" and nxt == "*":
            state = "block_comment"
            i += 2
        elif tri == '"""':
            state = "text_block"
            i += 3
        elif char == '"':
            state = "string"
            i += 1
        elif char == "'":
            state = "char"
            i += 1
        elif char in "([{":
            stack.append((char, line))
            i += 1
        elif char in ")]}":
            if not stack or stack[-1][0] != pairs[char]:
                fail(f"{path}:{line}: unmatched {char}")
            stack.pop()
            i += 1
        else:
            i += 1
    if state in {"block_comment", "string", "char", "text_block"}:
        fail(f"{path}: unclosed Java lexical state: {state}")
    if stack:
        char, opened = stack[-1]
        fail(f"{path}:{opened}: unclosed {char}")


def expanded_text(path: Path, numeric: bool = False) -> str:
    replacement = "1" if numeric else "x"
    return re.sub(r"\$\{[A-Za-z0-9_.-]+}", replacement, path.read_text(encoding="utf-8"))


def require_needles(root: Path, checks: dict[str, list[str]]) -> None:
    for relative, needles in checks.items():
        path = root / relative
        if not path.is_file():
            fail(f"missing required file: {relative}")
        text = path.read_text(encoding="utf-8")
        for needle in needles:
            if needle not in text:
                fail(f"missing expected 8.0.12 invariant {needle!r} in {relative}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-root", type=Path, default=Path("version-src/1.21.1-forge"))
    parser.add_argument("--target", default="1.21.1-forge")
    args = parser.parse_args()

    workspace = Path(__file__).resolve().parents[1]
    root = args.source_root.resolve()
    src = root / "src"
    if not (src / "main/java").is_dir() or not (src / "main/resources").is_dir():
        fail(f"invalid 1.21.1 source root: {root}")

    properties = load_properties(workspace / "stonecutter-targets.properties")
    prefix = f"target.{args.target}."
    expected = {
        "source.root": "version-src/1.21.1-forge",
        "deps.minecraft": "1.21.1",
        "deps.forge": "52.1.16",
        "java.version": "21",
        "network.protocol": "BC8.0.x-1.21.1-net2",
        "pack.format": "34",
        "pack.resource_format": "34",
        "pack.data_format": "48",
        "compat.jei.enabled": "true",
        "compat.jade.enabled": "false",
        "compat.ic2.enabled": "false",
        "compat.forestry.enabled": "false",
    }
    for key, value in expected.items():
        actual = properties.get(prefix + key)
        if actual != value:
            fail(f"{prefix + key} must be {value!r}, got {actual!r}")
    targets = [item.strip() for item in properties.get("targets", "").split(",") if item.strip()]
    if args.target not in targets:
        fail(f"{args.target} is not registered in targets")
    if properties.get("common.mod.version") != "8.0.12":
        fail("common.mod.version must be 8.0.12")

    required = [
        "PORTING.md",
        "src/main/java/buildcraft/lib/BCLib.java",
        "src/main/java/buildcraft/lib/net/MessageManager.java",
        "src/main/java/buildcraft/lib/net/MessageUpdateTile.java",
        "src/main/java/buildcraft/lib/item/ItemGuide.java",
        "src/main/java/buildcraft/lib/client/guide/GuiGuide.java",
        "src/main/java/buildcraft/compat/jei/BuildCraftJeiPlugin.java",
        "src/main/java/buildcraft/robotics/zone/ZonePlannerMapChunk.java",
        "src/main/java/buildcraft/transport/recipe/PipeRecipe.java",
        "src/main/resources/assets/buildcraft/guide/original_manifest.json",
        "src/main/resources/data/buildcraftcore/advancement/guide.json",
        "src/main/resources/data/buildcraftlib/recipe/guide_book.json",
    ]
    for relative in required:
        if not (root / relative).is_file():
            fail(f"missing 1.21.1/8.0.12 port file: {relative}")

    forbidden_directories = list((src / "main/resources/data").rglob("recipes"))
    forbidden_directories += list((src / "main/resources/data").rglob("advancements"))
    if forbidden_directories:
        fail("legacy plural data directories remain: " + ", ".join(map(str, forbidden_directories[:8])))

    if (src / "main/java/buildcraft/compat/forestry").exists():
        fail("Forestry Java sources must not be compiled for Forge 1.21.1")
    if (src / "main/java/buildcraft/compat/ic2").exists():
        fail("IC2 Java sources must not be present in the Forge 1.21.1 target")
    if (src / "main/java/buildcraft/compat/jade").exists():
        fail("Jade Java sources must not be present in the Forge 1.21.1 target")

    text_suffixes = {
        ".java", ".json", ".mcmeta", ".toml", ".properties", ".gradle",
        ".kts", ".yml", ".yaml", ".md", ".sh", ".py",
    }
    for path in (p for p in root.rglob("*") if p.is_file() and p.suffix.lower() in text_suffixes):
        text = path.read_text(encoding="utf-8", errors="strict")
        if re.search(r"^(?:<<<<<<<|=======|>>>>>>>)(?: .*)?$", text, flags=re.MULTILINE):
            fail(f"merge marker remains in {path}")
        if path.suffix.lower() != ".md" and "8.0.11" in text:
            fail(f"old 8.0.11 marker remains in {path}")

    java_root = src / "main/java"
    java_files = list(java_root.rglob("*.java"))
    for path in java_files:
        scan_java(path)
        text = path.read_text(encoding="utf-8")

        package_match = re.search(r"^\s*package\s+([A-Za-z_][\w.]*)\s*;", text, flags=re.MULTILINE)
        expected_package = ".".join(path.relative_to(java_root).parts[:-1])
        if package_match is None or package_match.group(1) != expected_package:
            fail(f"package declaration does not match source path in {path}")

        for imported in re.findall(
            r"^\s*import\s+(?:static\s+)?(buildcraft(?:\.[A-Za-z_$][\w$]*)+)\s*;",
            text,
            flags=re.MULTILINE,
        ):
            parts = imported.split(".")
            if not any(
                java_root.joinpath(*parts[:length]).with_suffix(".java").is_file()
                for length in range(len(parts), 1, -1)
            ):
                fail(f"unresolved internal BuildCraft import {imported!r} in {path}")
        if "new ResourceLocation(" in text:
            fail(f"removed ResourceLocation constructor remains in {path}")
        if re.search(r"(?<!Player)NetworkEvent(?:\.|<|\s)", text):
            fail(f"old Forge NetworkEvent API remains in {path}")
        if "//$" in text:
            fail(f"comment collides with Stonecutter swap syntax in {path}")
        if "trying to deserialize packet" in text or "//Debug start" in text:
            fail(f"temporary network debug logging remains in {path}")
        if "getCloneItemStack(null" in text:
            fail(f"unsafe null-context clone-stack call remains in {path}")

    for path in src.rglob("*.json"):
        try:
            document = json.loads(path.read_text(encoding="utf-8"))
        except Exception as exc:
            fail(f"invalid JSON {path}: {exc}")

        normalized = path.as_posix()
        if "/recipe/" in normalized and isinstance(document, dict):
            recipe_type = str(document.get("type", ""))
            result = document.get("result")
            if recipe_type.startswith("minecraft:") and isinstance(result, dict):
                if "item" in result:
                    fail(f"legacy recipe result key 'item' remains in {path}; Forge 1.21.1 uses 'id'")
        if "/advancement/" in normalized and isinstance(document, dict):
            display = document.get("display")
            icon = display.get("icon") if isinstance(display, dict) else None
            if isinstance(icon, dict) and "item" in icon:
                fail(f"legacy advancement icon key 'item' remains in {path}; Forge 1.21.1 uses 'id'")
    for path in src.rglob("*.mcmeta"):
        try:
            json.loads(expanded_text(path, numeric=True))
        except Exception as exc:
            fail(f"invalid mcmeta {path}: {exc}")
    for path in src.rglob("*.toml"):
        try:
            tomllib.loads(expanded_text(path))
        except Exception as exc:
            fail(f"invalid TOML template {path}: {exc}")

    require_needles(root, {
        "src/main/java/buildcraft/lib/BCLib.java": [
            'MC_VERSION = "1.21.1"',
        ],
        "src/main/java/buildcraft/lib/net/MessageManager.java": [
            "BuildCraftTarget.NETWORK_PROTOCOL",
            "Integer.MAX_VALUE",
            "CustomPayloadEvent.Context",
        ],
        "src/main/java/buildcraft/lib/net/MessageUpdateTile.java": [
            "MAX_PAYLOAD_SIZE",
            "hasChunkAt(message.pos)",
            "canInteractWith(sender)",
        ],
        "src/main/java/buildcraft/lib/net/cache/MessageObjectCacheResponse.java": [
            "MAX_IDS",
            "MAX_VALUE_SIZE",
            "readUnsignedShort",
        ],
        "src/main/java/buildcraft/builders/snapshot/MessageSnapshotResponse.java": [
            "MAX_COMPRESSED_SNAPSHOT_BYTES",
            "64L * 1024 * 1024",
            "NbtAccounter.create",
        ],
        "src/main/java/buildcraft/builders/snapshot/BlueprintBuilder.java": [
            "pendingFluidRefunds",
            "handleExcavationDrops",
            "getAutomationPlayer",
        ],
        "src/main/java/buildcraft/builders/snapshot/RequiredExtractorItemFromBlock.java": [
            "SingleBlockAccess access",
            "getCloneItemStack(",
            "if (result.isEmpty())",
        ],
        "src/main/java/buildcraft/core/client/RenderTickListener.java": [
            "event.getText()",
            "event.getSide()",
            "DebugText.Side.Left",
        ],
        "src/main/java/buildcraft/lib/block/BlockBCTile_Neptune.java": [
            "new ItemStack(state.getBlock().asItem())",
        ],
        "src/main/java/buildcraft/lib/misc/FluidUtilBC.java": [
            "FluidAction.SIMULATE",
            "FluidAction.EXECUTE",
            "restoreFluid",
        ],
        "src/main/java/buildcraft/transport/pipe/flow/PipeFlowPower.java": [
            "saturatingAdd",
            "receivePowerInternal",
        ],
        "src/main/java/buildcraft/transport/item/ItemPipeHolder.java": [
            "PIPE_COLOR_TAG",
            "ItemStackUtil.setCustomData",
        ],
        "src/main/java/buildcraft/transport/recipe/PipeRecipe.java": [
            "MapCodec<PipeRecipe>",
            "StreamCodec<RegistryFriendlyByteBuf, PipeRecipe>",
            "CraftingInput",
        ],
        "src/main/java/buildcraft/factory/block/BlockFloodGate.java": [
            "onOpenSidesChanged()",
        ],
        "src/main/java/buildcraft/robotics/zone/ZonePlannerMapChunk.java": [
            "getChunkNow",
        ],
        "src/main/java/buildcraft/robotics/zone/MessageZoneMapRequest.java": [
            "getChunkNow",
            "CustomPayloadEvent.Context",
        ],
        "src/main/java/buildcraft/silicon/client/render/RenderProgrammingTable.java": [
            "whiteStainedGlass.getU",
        ],
    })

    transport_provider = (root / "src/main/java/buildcraft/transport/BCTransportRecipesProvider.java").read_text(encoding="utf-8")
    build_body = re.search(
        r"protected void buildRecipes\(RecipeOutput writer\)\s*\{(.*?)\n\s*}",
        transport_provider,
        flags=re.DOTALL,
    )
    if not build_body:
        fail("cannot locate 1.21.1 BCTransportRecipesProvider.buildRecipes")
    if "creatPipeRecipes(writer" in build_body.group(1) or "creatSpecPipeRecipes(writer" in build_body.group(1):
        fail("1.21.1 data generator would overwrite hand-authored coloured pipe recipes")
    if "plugPowerAdaptor.get(), 4" not in transport_provider:
        fail("power-adapter recipe must output four adapters")

    mods_text = (src / "main/resources/META-INF/mods.toml").read_text(encoding="utf-8")
    if "forestry_version_range" in mods_text or 'modId="forestry"' in mods_text:
        fail("Forestry dependency metadata remains enabled on Forge 1.21.1")

    porting = (root / "PORTING.md").read_text(encoding="utf-8")
    for expected_text in ("8.0.12", "Forge 1.21.1", "beta", "Forestry"):
        if expected_text not in porting:
            fail(f"PORTING.md does not document {expected_text!r}")

    print(
        "BuildCraft 8.0.12 Forge 1.21.1 beta layout OK: "
        f"{len(java_files)} Java files, Forge=52.1.16, Java=21, protocol=BC8.0.x-1.21.1-net2"
    )


if __name__ == "__main__":
    main()
