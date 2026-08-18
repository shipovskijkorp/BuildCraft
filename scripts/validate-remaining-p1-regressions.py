#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
errors = []


def read(rel):
    path = ROOT / rel
    if not path.is_file():
        errors.append(f"missing {rel}")
        return ""
    return path.read_text(encoding="utf-8")


def require(rel, *tokens):
    text = read(rel)
    for token in tokens:
        if token not in text:
            errors.append(f"{rel}: missing remaining-P1 guard {token!r}")


def forbid(rel, *tokens):
    text = read(rel)
    for token in tokens:
        if token in text:
            errors.append(f"{rel}: forbidden remaining-P1 token {token!r}")


# 1.21.1 automation identity must be a real NeoForge FakePlayer, not a hand-rolled ServerPlayer.
require(
    "source-platforms/neoforge/src/main/java/buildcraft/lib/fake/FakePlayerBC.java",
    "import net.neoforged.neoforge.common.util.FakePlayer;",
    "public class FakePlayerBC extends FakePlayer",
    "super(level, profile);",
)
forbid(
    "source-platforms/neoforge/src/main/java/buildcraft/lib/fake/FakePlayerBC.java",
    "extends ServerPlayer",
    "DiscardingConnection",
    "ServerGamePacketListenerImpl",
)
for platform in ("forge", "neoforge"):
    require(
        f"source-platforms/{platform}/src/gametest/java/buildcraft/gametest/PermissionOwnerGameTests.java",
        "buildCraftAutomationPlayerUsesPlatformFakePlayer",
        "player instanceof FakePlayer",
    )

# 1.21 JEI must retain RecipeHolder identity instead of grouping every codec-default recipe under unnamed id.
jei = "source-platforms/neoforge/src/main/java/buildcraft/compat/jei/BuildCraftJeiPlugin.java"
require(
    jei,
    "private record AssemblyJeiRecipe(ResourceLocation id, AssemblyRecipeBasic recipe)",
    ".map(holder -> new AssemblyJeiRecipe(holder.id(), holder.value()))",
    "representative.id()",
    "GROUPED_ASSEMBLY_RECIPES.get(view.id())",
)
forbid(jei, ".map(RecipeHolder::value)", "GROUPED_ASSEMBLY_RECIPES.get(recipe.getId())")

# Picker reservations must be globally unambiguous across dimensions and runtime entity-id reuse.
require(
    "source-shared/src/main/java/buildcraft/robotics/boards/BoardRobotPicker.java",
    "record TargetKey(ResourceKey<Level> dimension, UUID itemUuid)",
    "item.getCommandSenderWorld().dimension()",
    "item.getUUID()",
)
for family in ("legacy", "modern"):
    rel = f"source-families/{family}/src/main/java/buildcraft/robotics/ai/AIRobotFetchItem.java"
    require(rel, "BoardRobotPicker.TargetKey.of(item)", "targetReservation", "targettedItems.add(targetReservation)")
    forbid(rel, "targettedItems.contains(item.getId())", "targettedItems.add(targetId)")

# Wire updates must be scoped to the actual player's tracked chunks, not global ticking-range state.
# The implementation is shared so Forge 1.19.2/1.20.1 and NeoForge cannot drift on Entity level access again.
rel = "source-shared/src/main/java/buildcraft/transport/wire/WireSystem.java"
require(
    rel,
    "//? if <1.20 {",
    "ServerLevel world = (ServerLevel) serverPlayer.level;",
    "ServerLevel world = serverPlayer.serverLevel();",
    "chunkMap.getPlayers(chunkPos, false).contains(serverPlayer)",
    ".distinct()",
)
forbid(rel, "inBlockTickingRange", "player.level instanceof ServerLevel", "player.level() instanceof ServerLevel")
for family in ("legacy", "modern"):
    stale = ROOT / f"source-families/{family}/src/main/java/buildcraft/transport/wire/WireSystem.java"
    if stale.exists():
        errors.append(f"{stale.relative_to(ROOT)}: stale WireSystem family override shadows the shared implementation")

# Bomber may not consume TNT or prime an explosion before zone/API2 checks for the affected area succeed.
for family in ("legacy", "modern"):
    rel = f"source-families/{family}/src/main/java/buildcraft/robotics/boards/BoardRobotBomber.java"
    require(
        rel,
        "BLAST_SAFETY_RADIUS = 6",
        "canBombTarget(target, OperationMode.SIMULATE)",
        "canBombTarget(bombTarget, OperationMode.EXECUTE)",
        "zone != null && !zone.contains(affected)",
        "WorldOperationKind.BLOCK_BREAK, mode",
        "private void dropTnt(BlockPos target)",
    )

if errors:
    print("ERROR: remaining P1 regression validation failed")
    for error in errors:
        print(" -", error)
    raise SystemExit(1)
print("OK: remaining P1 regression guards are present")
