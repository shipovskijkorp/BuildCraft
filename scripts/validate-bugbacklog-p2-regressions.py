#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
errors = []

def read(rel):
    p = ROOT / rel
    if not p.is_file():
        errors.append(f"missing {rel}")
        return ""
    return p.read_text(encoding="utf-8")

def require(rel, *tokens):
    text = read(rel)
    for token in tokens:
        if token not in text:
            errors.append(f"{rel}: missing P2 regression guard {token!r}")

def forbid(rel, *tokens):
    text = read(rel)
    for token in tokens:
        if token in text:
            errors.append(f"{rel}: forbidden P2 regression token {token!r}")

for family in ("legacy", "modern"):
    require(
        f"source-families/{family}/src/main/java/buildcraft/lib/block/BlockBCBase_Neptune.java",
        "b.canFaceVertically() && placer != null",
        "placer.getX()", "placer.getY()", "placer.getZ()",
    )
    forbid(
        f"source-families/{family}/src/main/java/buildcraft/lib/block/BlockBCBase_Neptune.java",
        "placer.xo", "placer.yo", "placer.zo",
    )
    require(
        f"source-families/{family}/src/main/java/buildcraft/robotics/ai/AIRobotBreak.java",
        'nbt.putFloat("blockDamage", blockDamage)',
        'nbt.getFloat("blockDamage")',
        "progressStateKey",
    )
    require(
        f"source-families/{family}/src/main/java/buildcraft/lib/crops/CropHandlerReeds.java",
        "state.getBlock() == Blocks.SUGAR_CANE",
        "CropHandlerPlantable.INSTANCE.harvest",
    )

require(
    "source-shared/src/main/java/buildcraft/lib/inventory/AbstractInvItemTransactor.java",
    "ItemStack remainder = asValid(item);",
    "remainder = insert(i, remainder, simulate);",
)

for platform in ("forge", "neoforge"):
    require(
        f"source-platforms/{platform}/src/main/java/buildcraft/factory/tile/TilePump.java",
        "level.hasChunkAt(offsetPos)",
        "level.hasChunkAt(spring)",
        "level.hasChunkAt(currentPos)",
    )
    require(
        f"source-platforms/{platform}/src/main/java/buildcraft/factory/tile/TileFloodGate.java",
        "level.hasChunkAt(toCheck)",
        "level.hasChunkAt(next)",
        "level.hasChunkAt(offsetPos)",
    )
    require(
        f"source-platforms/{platform}/src/main/java/buildcraft/transport/pipe/behaviour/PipeBehaviourStripes.java",
        'nbt.putLong("progress", progress)',
        "progressTarget",
        "progressStateKey",
        "matchesProgressTarget",
        "requiresPeriodicSave()",
    )
    require(
        f"source-platforms/{platform}/src/main/java/buildcraft/transport/tile/TilePipeHolder.java",
        "pipe.flow.requiresPeriodicSave() || pipe.behaviour.requiresPeriodicSave()",
    )
    require(
        f"source-platforms/{platform}/src/main/java/buildcraft/transport/pipe/flow/PipeFlowFluids.java",
        "s.forceDrain(amount)",
        "section.incomingTotalCache = 0",
        "trimDelayedFluidToAmount",
        "removeDelayedFluid",
        "Math.max(0, amount - incomingTotalCache)",
    )
    require(
        f"source-platforms/{platform}/src/main/java/buildcraft/energy/BCEnergyConfig.java",
        "return !destination.isEmpty();",
    )
    require(
        f"source-platforms/{platform}/src/main/java/buildcraft/lib/crops/CropHandlerPlantable.java",
        "BlockUtil.breakBlockAndGetDrops",
        "actor.getGameProfile()",
    )
    forbid(
        f"source-platforms/{platform}/src/main/java/buildcraft/lib/crops/CropHandlerPlantable.java",
        "serverLevel.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState())",
    )
    require(
        f"source-platforms/{platform}/src/gametest/java/buildcraft/gametest/BuildCraftLogicGameTests.java",
        "bulkItemTransactorInsertionCarriesRemainderAcrossSlots",
        "sugarCaneAdapterHarvestsOnlyGrowthAboveTheBase",
    )
    require(
        f"source-platforms/{platform}/src/gametest/java/buildcraft/transport/pipe/flow/PipeFluidPowerGameTests.java",
        "fullForceExtractionThenRefillDoesNotGhostJamPipe",
    )

if errors:
    print("ERROR: bug-backlog P2 regression validation failed")
    for error in errors:
        print(" -", error)
    raise SystemExit(1)
print("OK: bug-backlog P2 regression guards are present")
