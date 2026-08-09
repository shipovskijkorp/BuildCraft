#!/usr/bin/env python3
"""Offline guards for player-visible parity with the 1.19.2 Forge target."""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import Any

from source_layout import materialize_target

ROOT = Path(__file__).resolve().parents[1]
TARGETS = {
    target: materialize_target(target)
    for target in ("1.19.2-forge", "1.20.1-forge", "1.21.1-neoforge")
}
NEWER = ("1.20.1-forge", "1.21.1-neoforge")
ERRORS: list[str] = []


def fail(message: str) -> None:
    ERRORS.append(message)


def source(target: str, relative: str) -> Path:
    return TARGETS[target] / relative


def text(target: str, relative: str) -> str:
    path = source(target, relative)
    if not path.is_file():
        fail(f"{target}: missing {relative}")
        return ""
    return path.read_text(encoding="utf-8")


def require(target: str, relative: str, *needles: str) -> str:
    value = text(target, relative)
    for needle in needles:
        if needle not in value:
            fail(f"{target}: missing {needle!r} in {relative}")
    return value


def forbid(target: str, relative: str, *needles: str) -> str:
    value = text(target, relative)
    for needle in needles:
        if needle in value:
            fail(f"{target}: forbidden {needle!r} remains in {relative}")
    return value


def compact(value: str) -> str:
    return re.sub(r"\s+", " ", value).strip()


def require_compact(target: str, relative: str, *needles: str) -> str:
    value = compact(text(target, relative))
    for needle in needles:
        if compact(needle) not in value:
            fail(f"{target}: missing normalized fragment {needle!r} in {relative}")
    return value


def recipe_inventory(target: str) -> dict[str, tuple[str | None, int]]:
    data = TARGETS[target] / "src/main/resources/data"
    directory = "recipe" if target.startswith("1.21.1") else "recipes"
    result: dict[str, tuple[str | None, int]] = {}
    for namespace in sorted(p for p in data.iterdir() if p.is_dir()):
        root = namespace / directory
        if not root.is_dir():
            continue
        for path in sorted(root.rglob("*.json")):
            recipe_id = f"{namespace.name}:{path.relative_to(root).with_suffix('').as_posix()}"
            try:
                document = json.loads(path.read_text(encoding="utf-8"))
            except Exception as exc:
                fail(f"{target}: invalid recipe JSON {path.relative_to(TARGETS[target])}: {exc}")
                continue
            raw = document.get("result") if isinstance(document, dict) else None
            item: str | None = None
            count = 1
            if isinstance(raw, str):
                item = raw
            elif isinstance(raw, dict):
                candidate = raw.get("id", raw.get("item"))
                if isinstance(candidate, str):
                    item = candidate
                raw_count = raw.get("count", 1)
                if isinstance(raw_count, int):
                    count = raw_count
            result[recipe_id] = (item, count)
    return result


def loot_inventory(target: str) -> dict[str, Any]:
    data = TARGETS[target] / "src/main/resources/data"
    directory = "loot_table" if target.startswith("1.21.1") else "loot_tables"
    result: dict[str, Any] = {}
    for namespace in sorted(p for p in data.iterdir() if p.is_dir()):
        root = namespace / directory
        if not root.is_dir():
            continue
        for path in sorted(root.rglob("*.json")):
            loot_id = f"{namespace.name}:{path.relative_to(root).with_suffix('').as_posix()}"
            try:
                result[loot_id] = json.loads(path.read_text(encoding="utf-8"))
            except Exception as exc:
                fail(f"{target}: invalid loot-table JSON {path.relative_to(TARGETS[target])}: {exc}")
    return result


def validate_tick_cadence() -> None:
    rel = "src/main/java/buildcraft/transport/BCTransportEventDist.java"
    value = require("1.20.1-forge", rel, "WorldSavedDataWireSystems.get(event.level).tick();", "PipeItemMessageQueue.serverTick();")
    if "event.phase" in value:
        fail("1.20.1-forge: transport ticks must not filter START/END phases")
    require("1.21.1-neoforge", rel,
            "onWorldTick(LevelTickEvent.Pre event)",
            "onWorldTick(LevelTickEvent.Post event)",
            "onServerTick(ServerTickEvent.Pre event)",
            "onServerTick(ServerTickEvent.Post event)")


def validate_java_invariants() -> None:
    engine = "src/main/java/buildcraft/energy/tile/TileEngineIron_BC8.java"
    gate = "src/main/java/buildcraft/silicon/recipe/GateLogicChangeRecipe.java"
    mining = "src/main/java/buildcraft/factory/tile/TileMiningWell.java"
    wrench = "src/main/java/buildcraft/core/item/ItemWrench.java"
    paint = "src/main/java/buildcraft/core/item/ItemPaintbrush_BC8.java"
    snapshot = "src/main/java/buildcraft/builders/snapshot/ClientSnapshots.java"
    wire = "src/main/java/buildcraft/transport/wire/WireSystem.java"
    oil_generator = "src/main/java/buildcraft/energy/generation/features/OilGenerator.java"
    oil_structure = "src/main/java/buildcraft/energy/generation/features/OilStructure.java"
    fluid = "src/main/java/buildcraft/lib/fluid/BCFluid.java"
    frame = "src/main/java/buildcraft/builders/block/BlockFrame.java"

    for target in NEWER:
        require_compact(target, engine,
            "public int getTanks() { return 3; }",
            "case 1 -> tankFuel.getFluid(); case 2 -> tankCoolant.getFluid(); case 3 -> tankResidue.getFluid(); default -> FluidStack.EMPTY;",
            "public int getTankCapacity(int tank) { return 0; }",
            "public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return false; }")
        gate_text = require(target, gate, "if (!stack.isEmpty() && stack.getItem() instanceof ItemPluggableGate)")
        if "else if (!stack.isEmpty())" in gate_text:
            fail(f"{target}: gate-logic recipe rejects unrelated occupied slots")
        require_compact(target, mining,
            "int limit = BCCoreConfig.miningMaxDepth; int high = worldPosition.getY() + 64; return high < limit ? high : limit;")
        require(target, wrench,
            "result == InteractionResult.SUCCESS && player != null",
            "player.swingingArm = hand;")
        forbid(target, wrench, "result.consumesAction()", "player.swing(hand, true)")
        require(target, paint,
            "result != InteractionResult.SUCCESS",
            "return InteractionResult.FAIL;",
            "return InteractionResult.SUCCESS;")
        forbid(target, paint, "result.consumesAction()", "InteractionResult.sidedSuccess")
        require(target, snapshot, "if (1 == 1)", "return;")
        require(target, wire, "manager.inBlockTickingRange(ChunkPos.asLong(element.blockPos))")
        forbid(target, wire, "isPlayerWatchingChunk")
        forbid(target, oil_generator, "genOilInEveryVanillaBiomes", "genOilInEveryModBiomes")
        require(target, oil_structure, "state.blocksMotion()")
        forbid(target, oil_structure, "OilGenerator.createTube(start, Math.max(0")
        require(target, fluid,
            ".is(Blocks.NETHER_PORTAL)",
            ".is(Blocks.END_PORTAL)",
            ".is(Blocks.END_GATEWAY)",
            ".is(Blocks.STRUCTURE_VOID)",
            ".getFluidState().isEmpty()")
        require(target, frame, "Direction.fromDelta")
        forbid(target, frame, "if (d != null)")

    for target in NEWER:
        if target != "1.21.1-neoforge":
            require(target, "src/main/java/buildcraft/core/BCCoreItems.java",
                    'MARKER_CONNECTOR = ITEMS.register("marker_connector"')
        core_items = text(target, "src/main/java/buildcraft/core/BCCoreItems.java")
        marker_line = next((line for line in core_items.splitlines() if "MARKER_CONNECTOR" in line and "register" in line), "")
        if ".stacksTo(1)" not in marker_line:
            fail(f"{target}: marker connector must stack to one")

    require_compact("1.21.1-neoforge", "src/main/java/buildcraft/lib/recipe/AssemblyRecipe.java",
                    "public Set<ItemStack> getOutputPreviews() { return ImmutableSet.of(); }")

    for target in TARGETS:
        require(target, "src/main/java/buildcraft/builders/block/BlockConstructionMarker.java",
                "WrenchUtil.isWrench(held)")
    require("1.21.1-neoforge", "src/main/java/buildcraft/transport/pipe/SchematicBlockPipe.java",
            "worldPipe.getDefinition().identifier.toString()",
            "NBTUtilBC.writeEnum(worldPipe.getColour())")



def validate_fluid_filter_safety() -> None:
    array_filter = "src/main/java/buildcraft/lib/inventory/filter/ArrayFluidFilter.java"
    diamond_fluid = "src/main/java/buildcraft/transport/pipe/behaviour/PipeBehaviourDiamondFluid.java"
    diamond_wood = "src/main/java/buildcraft/transport/pipe/behaviour/PipeBehaviourWoodDiamond.java"

    for target in TARGETS:
        require(target, array_filter,
                "FluidUtil.getFluidContained(stacks.get(i)).orElse(FluidStack.EMPTY)",
                "if (!fluid.isEmpty() && fluid.getAmount() > 0)")
        array_text = text(target, array_filter)
        if re.search(r"getFluidContained\(stacks\.get\(i\)\).*?else\s+return;", array_text, re.DOTALL):
            fail(f"{target}: ArrayFluidFilter must skip empty/non-fluid slots instead of stopping at the first gap")

        require(target, diamond_fluid,
                "FluidUtil.getFluidContained(compareTo).orElse(FluidStack.EMPTY)",
                "if (target.isEmpty() || target.getAmount() <= 0)")
        forbid(target, diamond_fluid, "FluidUtil.getFluidContained(compareTo).get()")

        require(target, diamond_wood,
                "ArrayFluidFilter fluidFilter = new ArrayFluidFilter(filters.stacks);",
                "if (!fluidFilter.hasFilter())",
                "FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY)",
                "return FluidStack.EMPTY;")
        forbid(target, diamond_wood,
               "FluidUtil.getFluidContained(stack).get()",
               "return null;")

def validate_protection_invariants() -> None:
    stripes = "src/main/java/buildcraft/transport/stripes/StripesHandlerShears.java"
    require("1.21.1-neoforge", stripes,
            "import net.minecraft.server.level.ServerLevel;",
            "BlockUtil.canBreakBlock(serverLevel, pos, player)",
            "shearableBlock.onSheared(player, stack, world, pos)")
    forbid("1.21.1-neoforge", stripes, "shearableBlock.onSheared(null")


def validate_robot_invariants() -> None:
    rel = "src/main/java/buildcraft/robotics/entity/EntityRobot.java"
    for target in ("1.20.1-forge", "1.21.1-neoforge"):
        value = text(target, rel)
        dock = re.search(r"public void dock\(DockingStation station\)\s*\{(.*?)\n\s*}", value, re.DOTALL)
        if not dock:
            fail(f"{target}: cannot locate robot dock method")
        elif "mainStationReleasedManually = false" in dock.group(1):
            fail(f"{target}: docking must not clear the manual-release state")
    require("1.21.1-neoforge", rel,
            'tag.put("owner", writeOwnerProfile(owner));',
            'owner = tag.contains("owner") ? readOwnerProfile',
            "private static GameProfile readOwnerProfile",
            "private static CompoundTag writeOwnerProfile",
            "FakePlayerProvider.INSTANCE.getFakePlayer(",
            "serverLevel, getOwnerProfile(), blockPosition()")
    attack = re.search(
        r"public void attackTargetEntityWithCurrentItem\(Entity target\)\s*\{(.*?)\n\s*private static float applyAttributeModifier",
        text("1.21.1-neoforge", rel),
        re.DOTALL,
    )
    if not attack:
        fail("1.21.1-neoforge: cannot locate robot attack method")
    elif "getBuildCraftPlayer(serverLevel)" in attack.group(1):
        fail("1.21.1-neoforge: robot attacks must use the owner-aware fake player")
    forbid("1.21.1-neoforge", rel, "amount = damageEvent.getAmount();")


def validate_target_specific_persistence() -> None:
    tank_rel = "src/main/java/buildcraft/lib/fluid/Tank.java"
    tank = require("1.21.1-neoforge", tank_rel,
                   "FluidStack fluid = FluidCompatRegistry.canonicalize(FluidStackUtil.parseOptional(nbt));",
                   "this.fluid = fluid;")
    outer = tank.find("FluidStack fluid = FluidCompatRegistry.canonicalize(FluidStackUtil.parseOptional(nbt));")
    named = tank.find("if (nbt.contains(name")
    if outer < 0 or named < 0 or outer > named:
        fail("1.21.1-neoforge: tank fluid must be parsed from the outer tag before the named child")

    pipe_rel = "src/main/java/buildcraft/transport/pipe/flow/PipeFlowItems.java"
    require("1.21.1-neoforge", pipe_rel,
            "private ListTag pendingItems;",
            "private final ListTag unreadableItems = new ListTag();",
            "pendingItems = nbt.getList(\"items\", Tag.TAG_COMPOUND).copy();",
            "private void loadPendingItems()",
            "if (level == null)",
            "pendingItems.copy()",
            "new TravellingItem(itemTag, tickNow, level.registryAccess())")


def validate_legacy_itemstack_migration() -> None:
    rel = "src/main/java/buildcraft/lib/misc/ItemStackUtil.java"
    require("1.21.1-neoforge", rel,
            "CompoundTag normalized = normalizeLegacyNbt(tag);",
            'normalized.contains("Count", Tag.TAG_ANY_NUMERIC)',
            'components.put("minecraft:custom_data", legacyData.copy());',
            'fallback.set(DataComponents.DAMAGE, Math.max(0, legacyData.getInt("Damage")));')


def validate_persistence_and_reload_invariants() -> None:
    # Saved Builder/Filler tasks already own reserved resources. Reload must cancel them through the
    # normal virtual cancellation path before rescanning the live world.
    snapshot_rel = "src/main/java/buildcraft/builders/snapshot/SnapshotBuilder.java"
    snapshot = require("1.21.1-neoforge", snapshot_rel,
                       'NBTUtilBC.readCompoundList(nbt.get("breakTasks"))',
                       ".forEach(this::cancelBreakTask);",
                       'NBTUtilBC.readCompoundList(nbt.get("placeTasks"))',
                       ".map(tag -> new PlaceTask(tag, registries))",
                       ".forEach(this::cancelPlaceTask);",
                       "forceRecheckCurrentTask();",
                       "flushPendingPowerRefund();")
    refund_pos = snapshot.find('NBTUtilBC.readCompoundList(nbt.get("breakTasks"))')
    recheck_pos = snapshot.find("forceRecheckCurrentTask();", refund_pos)
    if refund_pos < 0 or recheck_pos < 0 or refund_pos > recheck_pos:
        fail("1.21.1-neoforge: saved builder tasks must be refunded before forcing a rescan")

    # Unknown/custom pipe payloads must survive missing registrations and unchecked migration/codec failures.
    holder_rel = "src/main/java/buildcraft/transport/tile/TilePipeHolder.java"
    require("1.21.1-neoforge", holder_rel,
            "else if (unknownData != null)",
            'nbt.put("pipe", unknownData.copy());',
            'CompoundTag pipeData = nbt.getCompound("pipe");',
            "catch (InvalidInputDataException | RuntimeException e)",
            "unknownData = pipeData.copy();")
    forbid("1.21.1-neoforge", holder_rel, "unknownData = nbt.copy();")

    # Deferred blueprint inventory slots are local to each block entity. Combined double-chest capabilities
    # would merge both halves and make local slots 0-26 ambiguous.
    schematic_rel = "src/main/java/buildcraft/builders/snapshot/SchematicBlockDefault.java"
    for target in NEWER:
        schematic = require(target, schematic_rel,
                            "blockEntity instanceof Container container",
                            "new InvWrapper(container)",
                            "getDeferredInventoryHandler")
        if schematic.count("getDeferredInventoryHandler(") < 3:
            fail(f"{target}: deferred blueprint inventory helper must be used by both missing-item and insert paths")

    # The legacy path deliberately blocks tank -> container transfer in creative. The 1.21 vanilla bucket
    # special-case must do the same before it executes a real drain.
    fluid_rel = "src/main/java/buildcraft/lib/misc/FluidUtilBC.java"
    for target in ("1.21.1-neoforge",):
        fluid = text(target, fluid_rel)
        branch = re.search(
            r"if \(held\.is\(Items\.BUCKET\)\) \{(.*?)// Fragile shards",
            fluid,
            re.DOTALL,
        )
        if not branch:
            fail(f"{target}: cannot locate vanilla empty-bucket tank interaction")
            continue
        body = branch.group(1)
        creative = body.find("if (player.isCreative())")
        client_only_result = body.find("return player.level().isClientSide;", creative)
        execute_drain = body.find("FluidAction.EXECUTE")
        if creative < 0 or client_only_result < 0 or execute_drain < 0 or client_only_result > execute_drain:
            fail(f"{target}: creative empty buckets must be client-acknowledged but server-blocked before tank drain")

    # Distiller refunds and progress are persistent state, including corruption-safe non-negative clamps.
    distiller_rel = "src/main/java/buildcraft/factory/tile/TileDistiller.java"
    require("1.21.1-neoforge", distiller_rel,
            'distillPower = Math.max(0, nbt.getLong("distillPower"));',
            'pendingPowerRefund = Math.max(0, nbt.getLong("pendingPowerRefund"));')

    # Resource reloads invalidate expression nodes before reparsing variable models.
    event_rel = "src/main/java/buildcraft/lib/BCLibEventDist.java"
    for target in ("1.20.1-forge",):
        event_text = compact(text(target, event_rel))
        sequence = compact("ModelVariableData.onModelBake(); ModelHolderRegistry.reloadVariableModels();")
        if sequence not in event_text:
            fail(f"{target}: variable-model generation must advance before variable models are reparsed")

    # NeoForge's stitched-atlas event must invalidate sprite objects cached across resource generations.
    require("1.21.1-neoforge", event_rel,
            "SpriteUtil.clearAtlasCache();",
            "DebugRenderHelper.clearTextureCache();")


def validate_worldgen_resources() -> None:
    relatives = (
        "src/main/resources/data/buildcraftenergy/worldgen/configured_feature/oil_configured_feature.json",
        "src/main/resources/data/buildcraftenergy/worldgen/configured_feature/oil_configured_feature.jsonx",
        "src/main/resources/data/buildcraftenergy/worldgen/placed_feature/oil_placed_feature.json",
    )
    for target in NEWER:
        for relative in relatives:
            path = source(target, relative)
            try:
                document = json.loads(path.read_text(encoding="utf-8"))
            except Exception as exc:
                fail(f"{target}: invalid oil worldgen JSON {relative}: {exc}")
                continue
            serialized = json.dumps(document, sort_keys=True)
            if '"genOilInEveryModBiomes": false' not in serialized:
                fail(f"{target}: genOilInEveryModBiomes must stay false in {relative}")


def validate_advancements() -> None:
    for target in ("1.21.1-neoforge",):
        data = TARGETS[target] / "src/main/resources/data"
        all_files = sorted(data.glob("*/advancement/**/*.json"))
        recipe_files = sorted(data.glob("*/advancement/recipe/**/*.json"))
        if len(all_files) != 191:
            fail(f"{target}: expected 191 advancements, found {len(all_files)}")
        if len(recipe_files) != 147:
            fail(f"{target}: expected 147 recipe advancements, found {len(recipe_files)}")
        recipe_ids = set(recipe_inventory(target))
        referenced_recipes: set[str] = set()
        for path in all_files:
            try:
                document = json.loads(path.read_text(encoding="utf-8"))
            except Exception as exc:
                fail(f"{target}: invalid advancement JSON {path.relative_to(TARGETS[target])}: {exc}")
                continue
            if not isinstance(document, dict):
                fail(f"{target}: advancement root is not an object: {path.relative_to(TARGETS[target])}")
                continue
            rewards = document.get("rewards")
            if isinstance(rewards, dict) and isinstance(rewards.get("recipes"), list):
                referenced_recipes.update(item for item in rewards["recipes"] if isinstance(item, str))
        missing_references = sorted(referenced_recipes - recipe_ids)
        if missing_references:
            fail(f"{target}: advancement rewards reference missing recipes: {missing_references[:8]}")


def validate_resource_parity() -> None:
    recipes = {target: recipe_inventory(target) for target in TARGETS}
    if recipes["1.20.1-forge"] != recipes["1.19.2-forge"]:
        missing = sorted(set(recipes["1.19.2-forge"]) - set(recipes["1.20.1-forge"]))
        extra = sorted(set(recipes["1.20.1-forge"]) - set(recipes["1.19.2-forge"]))
        changed = sorted(k for k in recipes["1.19.2-forge"].keys() & recipes["1.20.1-forge"].keys()
                         if recipes["1.19.2-forge"][k] != recipes["1.20.1-forge"][k])
        fail(f"1.20.1-forge: recipe output inventory differs; missing={missing[:4]}, extra={extra[:4]}, changed={changed[:4]}")

    expected_121 = dict(recipes["1.19.2-forge"])
    expected_121.pop("buildcraftcompat:pipe_item_propolis", None)
    for target in ("1.21.1-neoforge",):
        if recipes[target] != expected_121:
            missing = sorted(set(expected_121) - set(recipes[target]))
            extra = sorted(set(recipes[target]) - set(expected_121))
            changed = sorted(k for k in expected_121.keys() & recipes[target].keys()
                             if expected_121[k] != recipes[target][k])
            fail(f"{target}: recipe output inventory differs; missing={missing[:4]}, extra={extra[:4]}, changed={changed[:4]}")

    loot = {target: loot_inventory(target) for target in TARGETS}
    baseline_ids = set(loot["1.19.2-forge"])
    if len(baseline_ids) != 30:
        fail(f"1.19.2-forge: expected 30 loot tables, found {len(baseline_ids)}")
    for target in NEWER:
        if set(loot[target]) != baseline_ids:
            fail(f"{target}: loot-table ID inventory differs from 1.19.2")
        elif loot[target] != loot["1.19.2-forge"]:
            fail(f"{target}: loot-table contents differ from 1.19.2")





def validate_gameplay_gap_fixes() -> None:
    pump = "src/main/java/buildcraft/factory/tile/TilePump.java"
    snapshot = "src/main/java/buildcraft/builders/snapshot/SnapshotBuilder.java"
    template = "src/main/java/buildcraft/builders/snapshot/TemplateBuilder.java"
    block_util = "src/main/java/buildcraft/lib/misc/BlockUtil.java"
    frame = "src/main/java/buildcraft/builders/block/BlockFrame.java"
    schematic = "src/main/java/buildcraft/builders/snapshot/SchematicBlockDefault.java"

    for target in TARGETS:
        require(target, pump,
                "INFINITE_WATER_NEIGHBORS",
                "isInfiniteWaterSourceAt(posToCheck)",
                "neighbour.isSource()",
                "adjacentSources >= 2")
        pump_text = text(target, pump)
        neighbor_decl = re.search(
            r"INFINITE_WATER_NEIGHBORS\s*=\s*new Direction\[\]\s*\{([^}]*)\}",
            pump_text,
            re.DOTALL,
        )
        if neighbor_decl is None:
            fail(f"{target}: cannot inspect infinite-water neighbour set")
        else:
            directions = neighbor_decl.group(1)
            for direction in ("Direction.NORTH", "Direction.SOUTH", "Direction.WEST", "Direction.EAST"):
                if direction not in directions:
                    fail(f"{target}: infinite-water detection is missing horizontal neighbour {direction}")
            for direction in ("Direction.UP", "Direction.DOWN"):
                if direction in directions:
                    fail(f"{target}: infinite-water detection must not count vertical neighbour {direction}")

        require(target, snapshot,
                "else if (canPlace(blockPos))",
                "checkResults[i] = CHECK_RESULT_TO_PLACE;")
        snapshot_text = text(target, snapshot)
        place_index = snapshot_text.find("else if (canPlace(blockPos))")
        break_index = snapshot_text.find("else if (!tile.getWorldBC().isEmptyBlock(blockPos))")
        if place_index < 0 or break_index < 0 or place_index > break_index:
            fail(f"{target}: replaceable placement must be classified before excavation")

        require(target, template, "BlockUtil.isReplaceable(tile.getWorldBC(), blockPos)")
        require(target, block_util, "public static boolean isReplaceable(Level world, BlockPos pos)")
        require(target, schematic,
                "getPlacementState(Level level, BlockPos blockPos)",
                ".canSurvive(level, blockPos)",
                "BlockStateProperties.PERSISTENT",
                "newBlockState.setValue(BlockStateProperties.PERSISTENT, true)",
                "if (!newBlockState.canSurvive(level, blockPos))",
                "BlockState expectedState = getPlacementState(world, blockPos);",
                "BlockUtil.blockStatesWithoutBlockEqual(expectedState, blockState2, ignoredProperties)")
        forbid(target, schematic,
               "newBlockState = newBlockState.rotate(level, blockPos, tileRotation);")

    for target in ("1.20.1-forge", "1.21.1-neoforge"):
        require(target, frame, ".forceSolidOn()")


def validate_modpack_interop_fixes() -> None:
    wrench_util = "src/main/java/buildcraft/lib/misc/WrenchUtil.java"
    core_config = "src/main/java/buildcraft/core/BCCoreConfig.java"
    pipe_holder = "src/main/java/buildcraft/transport/block/BlockPipeHolder.java"

    for target in TARGETS:
        wrench_text = require(target, wrench_util,
                'WRENCH_TAG_NAMESPACE = "c"',
                'WRENCH_TAG_PATH = "tools/wrench"',
                "stack.getItem() instanceof IToolWrench",
                "BCLibConfig.useWrenchTag && stack.getTags().anyMatch")
        legacy_wrench = wrench_text.find("stack.getItem() instanceof IToolWrench")
        tagged_wrench = wrench_text.find("BCLibConfig.useWrenchTag && stack.getTags().anyMatch")
        if legacy_wrench < 0 or tagged_wrench < 0 or legacy_wrench > tagged_wrench:
            fail(f"{target}: IToolWrench fallback must remain independent of the common-tag config toggle")
        require(target, core_config,
                '.define("useWrenchTag", true)',
                "BCLibConfig.useWrenchTag = propUseWrenchTag.get();")
        require(target, pipe_holder,
                "implements ICustomPaintHandler, EntityBlock, SimpleWaterloggedBlock",
                "BlockStateProperties.WATERLOGGED",
                "builder.add(WATERLOGGED)",
                "fluid.getType() == Fluids.WATER",
                "Fluids.WATER.getSource(false)",
                "world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world))")

    legacy_tag = ROOT / "source-families/legacy/src/main/resources/data/c/tags/items/tools/wrench.json"
    modern_tag = ROOT / "source-families/modern/src/main/resources/data/c/tags/item/tools/wrench.json"
    for tag_path in (legacy_tag, modern_tag):
        if not tag_path.is_file():
            fail(f"missing common wrench tag: {tag_path.relative_to(ROOT)}")
        elif '"buildcraftcore:wrench"' not in tag_path.read_text(encoding="utf-8"):
            fail(f"common wrench tag does not contain BuildCraft wrench: {tag_path.relative_to(ROOT)}")

    blockstate = ROOT / "source-shared/src/main/resources/assets/buildcrafttransport/blockstates/pipe_holder.json"
    if blockstate.is_file():
        blockstate_text = blockstate.read_text(encoding="utf-8")
        for variant in ('"waterlogged=false"', '"waterlogged=true"'):
            if variant not in blockstate_text:
                fail(f"pipe holder blockstate is missing {variant}")
    else:
        fail("missing shared pipe holder blockstate")

    # Waterlogging changes the baked block-model keys. Every target must replace the
    # two real blockstate variants with ModelPipe; replacing only the old empty
    # variant leaves the JSON fallback model in-world and breaks pipe/pluggable rendering.
    transport_models = "src/main/java/buildcraft/transport/BCTransportModels.java"
    for target in TARGETS:
        require(target, transport_models,
                'putModel(event, "pipe_holder#waterlogged=false", ModelPipe.INSTANCE);',
                'putModel(event, "pipe_holder#waterlogged=true", ModelPipe.INSTANCE);')
        forbid(target, transport_models,
               'putModel(event, "pipe_holder", ModelPipe.INSTANCE);')

    # Double-chest pairing is runtime-managed. The first half can temporarily revert to
    # SINGLE before the neighbour exists, but placement must still preserve the captured
    # LEFT/RIGHT state so the final pair can form. Therefore this exception belongs only
    # in structural equality, not in the generic ignoredProperties rule list.
    schematic = "src/main/java/buildcraft/builders/snapshot/SchematicBlockDefault.java"
    for target in TARGETS:
        require(target, schematic,
                "expectedState.hasProperty(BlockStateProperties.CHEST_TYPE)",
                "blockState2.hasProperty(BlockStateProperties.CHEST_TYPE)",
                "comparisonIgnored.add(BlockStateProperties.CHEST_TYPE)",
                "BlockUtil.blockStatesWithoutBlockEqual(expectedState, blockState2, comparisonIgnored)")

    inventory_rules = ROOT / (
        "source-shared/src/main/resources/assets/buildcraftbuilders/compat/buildcraft/"
        "builders/vanilla/blocks_simple_inventories.json"
    )
    if not inventory_rules.is_file():
        fail("missing shared simple-inventory builder rules")
    else:
        try:
            inventory_doc = json.loads(inventory_rules.read_text(encoding="utf-8"))
        except Exception as exc:
            fail(f"invalid simple-inventory builder rules: {exc}")
        else:
            selectors = inventory_doc[0].get("selectors", []) if isinstance(inventory_doc, list) and inventory_doc and isinstance(inventory_doc[0], dict) else []
            for chest in ("minecraft:chest", "minecraft:trapped_chest"):
                if chest not in selectors:
                    fail(f"simple-inventory builder rules are missing {chest}")
            ignored = inventory_doc[0].get("ignoredProperties", []) if isinstance(inventory_doc, list) and inventory_doc and isinstance(inventory_doc[0], dict) else []
            if "type" in ignored:
                fail("chest 'type' must not be a generic ignoredProperties entry because that also rewrites placement state")


def validate_jei_facade_scalability() -> None:
    plugin = "src/main/java/buildcraft/compat/jei/BuildCraftJeiPlugin.java"
    creative = "src/main/java/buildcraft/lib/CreativeTabManager.java"
    for target in TARGETS:
        require(target, plugin,
                'return data.getCompound("facade").toString();',
                "collectJeiFacadeInfos()",
                "unique.putIfAbsent(new ItemStackKey(required), info)",
                "recipe.inputStacks()",
                "recipe.solidOutputs()",
                "recipe.hollowOutputs()",
                "builder.createFocusLink(facadeInputSlot, solidOutputSlot, hollowOutputSlot)")
        forbid(target, plugin,
               'return instance.isHollow() ? "hollow" : "solid";',
               'return instance.isHollow() ? "phased_hollow" : "phased_solid";',
               "getFocusedFacadeInputInfos",
               "getRepresentativeFacadeInfo()")
        require(target, creative,
                "Set<ItemStackKey> seen = new HashSet<>();",
                "seen.add(new ItemStackKey(normalized))")
        forbid(target, creative,
               "for (ItemStack existing : items) {\n                if (ItemStack.isSameItemSame")

    hidden_tags = (
        ROOT / "source-families/legacy/src/main/resources/data/c/tags/items/hidden_from_recipe_viewers.json",
        ROOT / "source-families/modern/src/main/resources/data/c/tags/item/hidden_from_recipe_viewers.json",
    )
    for tag_path in hidden_tags:
        if not tag_path.is_file():
            fail(f"missing facade recipe-viewer visibility tag: {tag_path.relative_to(ROOT)}")
        elif '"buildcraftsilicon:plug/facade"' not in tag_path.read_text(encoding="utf-8"):
            fail(f"facade item missing from recipe-viewer visibility tag: {tag_path.relative_to(ROOT)}")


def validate_forestry_model_bake_mutation() -> None:
    rel = "src/main/java/buildcraft/compat/forestry/pipe/client/ForestryCompatClient.java"
    require("1.20.1-forge", rel,
            "private static void onModelBake(ModelEvent.ModifyBakingResult event)",
            "event.getModels().put(")
    forbid("1.20.1-forge", rel,
           "private static void onModelBake(ModelEvent.BakingCompleted event)")


def validate_forge_atlas_reload_caches() -> None:
    transport = "src/main/java/buildcraft/transport/BCTransportEventDist.java"
    silicon = "src/main/java/buildcraft/silicon/BCSilicon.java"

    # Forge's stitch event hands us the atlas generation that has just been rebuilt.
    # Dynamic BuildCraft quads cache absolute atlas UVs, so never repopulate the pipe
    # sprite table by querying Minecraft's global ModelManager from BakingCompleted.
    for target in ("1.20.1-forge",):
        require(target, transport,
                "TextureStitchEvent.Post",
                "InventoryMenu.BLOCK_ATLAS.equals(event.getAtlas().location())",
                "PipeBaseModelGenStandard.loadSpritesCache(event.getAtlas())",
                "clearAtlasDependentPipeCaches();")
        forbid(target, transport, "PipeBaseModelGenStandard.loadSpritesCache();")

        # Gates, lenses, facades and other pluggables also bake atlas-relative UVs.
        # Their caches must be invalidated for every fresh block-atlas generation.
        require(target, silicon,
                "TextureStitchEvent.Post",
                "InventoryMenu.BLOCK_ATLAS.equals(event.getAtlas().location())",
                "BCSiliconModels.clearAtlasDependentCaches();")

    # Keep the same invariant explicit for the reference/NeoForge paths so future
    # source-family moves do not accidentally remove the already-correct handlers.
    require("1.19.2-forge", transport,
            "TextureStitchEvent.Post",
            "PipeBaseModelGenStandard.loadSpritesCache(event.getAtlas())")
    require("1.19.2-forge", silicon,
            "TextureStitchEvent.Post",
            "BCSiliconModels.clearAtlasDependentCaches();")
    require("1.21.1-neoforge", transport,
            "TextureAtlasStitchedEvent",
            "PipeBaseModelGenStandard.loadSpritesCache(event.getAtlas())")
    require("1.21.1-neoforge", silicon,
            "TextureAtlasStitchedEvent",
            "BCSiliconModels.clearAtlasDependentCaches();")


def validate_pipe_pluggable_contract() -> None:
    pluggable = "src/main/java/buildcraft/api/transport/pluggable/PipePluggable.java"
    for target in TARGETS:
        require(target, pluggable,
                "public boolean isSideSolid()",
                "public float getExplosionResistance(@Nullable Entity exploder, Explosion explosion)",
                "public boolean canConnectToRedstone(@Nullable Direction to)")

    plugin = "src/main/java/buildcraft/compat/jei/BuildCraftJeiPlugin.java"
    require("1.19.2-forge", plugin, "return super.getResultItem();")
    forbid("1.19.2-forge", plugin, "super.getResultItem(registryAccess)")
    require("1.21.1-neoforge", plugin, "BCTransportItems.PIPE_STRUCTURE.isBound()")
    forbid("1.21.1-neoforge", plugin, "BCTransportItems.PIPE_STRUCTURE.isPresent()")


def validate_gametest_runtime_guards() -> None:
    expected_tests = 41
    for target in TARGETS:
        test_root = TARGETS[target] / "src/gametest/java"
        count = 0
        if test_root.is_dir():
            for path in test_root.rglob("*.java"):
                count += path.read_text(encoding="utf-8").count("@GameTest(")
        if count != expected_tests:
            fail(f"{target}: expected {expected_tests} @GameTest methods, found {count}")

    # GameTest classes must be part of the same exploded module as main during
    # runGameTestServer. Separate main/gameTest modules create split packages on
    # modern loader runtimes, while the reflection registrar cannot see the second
    # module on older Forge. The Gradle files are checked directly below.
    forge_gradle = (ROOT / "build.forge.gradle").read_text(encoding="utf-8")
    require_tokens = (
        "def gameTestRunRequested",
        "layeredDirs('src/gametest/java').each { java.srcDir(it) }",
        "layeredDirs('src/gametest/resources').each { resources.srcDir(it) }",
    )
    for token in require_tokens:
        if token not in forge_gradle:
            fail(f"build.forge.gradle: missing GameTest one-module guard: {token}")
    if "source sourceSets.gameTest" in forge_gradle:
        fail("build.forge.gradle: GameTest runtime still loads sourceSets.gameTest as a second module")

    neoforge_gradle = (ROOT / "build.neoforge.gradle").read_text(encoding="utf-8")
    for token in (
        "def gameTestRunRequested",
        "layeredDirs('src/gametest/java').each { java.srcDir(it) }",
        "layeredDirs('src/gametest/resources').each { resources.srcDir(it) }",
        "sourceSet = sourceSets.main",
    ):
        if token not in neoforge_gradle:
            fail(f"build.neoforge.gradle: missing GameTest one-module guard: {token}")
    if "sourceSet(sourceSets.gameTest)" in neoforge_gradle:
        fail("build.neoforge.gradle: mod still exposes gameTest as a second source set")

    for target in ("1.19.2-forge", "1.20.1-forge"):
        bclib = text(target, "src/main/java/buildcraft/lib/BCLib.java")
        if "BuildCraftGameTestRegistrar" in bclib:
            fail(f"{target}: obsolete reflection GameTest registrar remains wired into BCLib")
        registrar = TARGETS[target] / "src/main/java/buildcraft/lib/BuildCraftGameTestRegistrar.java"
        if registrar.exists():
            fail(f"{target}: obsolete reflection GameTest registrar source still exists")

    for target in ("1.20.1-forge", "1.21.1-neoforge"):
        silicon = text(target, "src/main/java/buildcraft/silicon/BCSilicon.java")
        for forbidden in ("BCSiliconConfig::onLoadConfig", "BCSiliconConfig::onReloadConfig", "BCSiliconConfig.reloadConfig(MODID)"):
            if forbidden in silicon:
                fail(f"{target}: silicon laser config diverges from 1.19.2 reference: {forbidden}")
    neo_silicon = text("1.21.1-neoforge", "src/main/java/buildcraft/silicon/BCSilicon.java")
    for forbidden in ("BCSiliconConfig.preInit()", "registerConfig(Type.COMMON, BCSiliconConfig.config)"):
        if forbidden in neo_silicon:
            fail(f"1.21.1-neoforge: silicon config remains active unlike 1.19.2: {forbidden}")

    for target in ("1.21.1-neoforge",):
        base = TARGETS[target] / "src/gametest/resources/data/buildcraftlib"
        modern = base / "structure"
        legacy = base / "structures"
        for name in ("empty3x3x3.nbt", "empty7x3x7.nbt"):
            if not (modern / name).is_file():
                fail(f"{target}: missing modern GameTest structure data/buildcraftlib/structure/{name}")
        if legacy.exists():
            fail(f"{target}: obsolete plural GameTest structure directory remains: {legacy.relative_to(TARGETS[target])}")

    neo = TARGETS["1.21.1-neoforge"]
    resources = neo / "src/main/resources"
    strict_count = 0
    for path in resources.rglob("*.json"):
        try:
            document = json.loads(path.read_text(encoding="utf-8"))
        except Exception:
            continue

        def walk(value: Any) -> None:
            nonlocal strict_count
            if isinstance(value, dict):
                ingredient_type = value.get("type")
                if ingredient_type == "c:nbt":
                    fail(f"1.21.1-neoforge: unsupported c:nbt ingredient remains in {path.relative_to(neo)}")
                if ingredient_type == "buildcraftlib:strict_nbt":
                    strict_count += 1
                    if not isinstance(value.get("item"), str):
                        fail(f"1.21.1-neoforge: strict-NBT ingredient has no item in {path.relative_to(neo)}")
                    if "nbt" in value and not isinstance(value.get("nbt"), str):
                        fail(f"1.21.1-neoforge: strict-NBT ingredient has non-string NBT in {path.relative_to(neo)}")
                for child in value.values():
                    walk(child)
            elif isinstance(value, list):
                for child in value:
                    walk(child)

        walk(document)
        if isinstance(document, dict) and document.get("type") == "buildcraftlib:assembly":
            ingredients = document.get("ingredients")
            counts = document.get("ingredient_counts")
            if isinstance(ingredients, list) and isinstance(counts, list) and len(ingredients) != len(counts):
                fail(f"1.21.1-neoforge: assembly ingredient/count length mismatch in {path.relative_to(neo)}")

    if strict_count != 26:
        fail(f"1.21.1-neoforge: expected 26 legacy strict-NBT recipe ingredients, found {strict_count}")

    require("1.21.1-neoforge", "src/main/java/buildcraft/lib/recipe/LegacyStrictNbtIngredient.java",
            "ItemStack.isSameItemSameComponents(input, displayStack)",
            "ItemStack count is deliberately ignored")
    require("1.21.1-neoforge", "src/main/java/buildcraft/lib/recipe/BCLibIngredientTypes.java",
            'INGREDIENT_TYPES.register("strict_nbt"',
            "NeoForgeRegistries.Keys.INGREDIENT_TYPES")

def main() -> None:
    for target, root in TARGETS.items():
        if not (root / "src/main/java").is_dir():
            fail(f"{target}: invalid target source root {root}")

    validate_tick_cadence()
    validate_java_invariants()
    validate_fluid_filter_safety()
    validate_protection_invariants()
    validate_robot_invariants()
    validate_target_specific_persistence()
    validate_legacy_itemstack_migration()
    validate_persistence_and_reload_invariants()
    validate_worldgen_resources()
    validate_advancements()
    validate_resource_parity()
    validate_gameplay_gap_fixes()
    validate_modpack_interop_fixes()
    validate_jei_facade_scalability()
    validate_forestry_model_bake_mutation()
    validate_forge_atlas_reload_caches()
    validate_pipe_pluggable_contract()
    validate_gametest_runtime_guards()

    if ERRORS:
        for error in ERRORS:
            print(f"ERROR: {error}", file=sys.stderr)
        raise SystemExit(1)

    print(
        "Behavior parity OK: 2 newer targets locked to the 1.19.2 reference; "
        "220/219 recipes, 30 loot tables and 191 advancements validated"
    )


if __name__ == "__main__":
    main()
