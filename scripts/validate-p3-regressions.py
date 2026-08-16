#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
errors = []


def text(rel):
    path = ROOT / rel
    if not path.is_file():
        errors.append(f"missing {rel}")
        return ""
    return path.read_text(encoding="utf-8")


def require(rel, *tokens):
    source = text(rel)
    for token in tokens:
        if token not in source:
            errors.append(f"{rel}: missing P3 guard {token!r}")


for platform in ("forge", "neoforge"):
    base = f"source-platforms/{platform}/src/main/java"
    require(
        f"{base}/buildcraft/lib/tile/craft/WorkbenchCrafting.java",
        "if (!clearInventory())",
        "matchingRecipes",
        "selectRecipe(int delta)",
        "writeSelection(CompoundTag nbt)",
        "readSelection(CompoundTag nbt)",
        "selectedCraftingRecipe",
    )
    require(
        f"{base}/buildcraft/factory/tile/TileAutoWorkbenchBase.java",
        "crafting.writeSelection(nbt)",
        "crafting.readSelection(nbt)",
        "cycleRecipe(int delta)",
        "getRecipeSelectionCount()",
    )
    require(
        f"{base}/buildcraft/silicon/tile/TileAdvancedCraftingTable.java",
        "crafting.writeSelection(nbt)",
        "crafting.readSelection(nbt)",
        "cycleRecipe(int delta)",
        "getRecipeSelectionCount()",
    )
    require(
        f"{base}/buildcraft/factory/container/ContainerAutoCraftItems.java",
        "BUTTON_PREVIOUS_RECIPE",
        "BUTTON_NEXT_RECIPE",
        "tile.cycleRecipe(-1)",
        "tile.cycleRecipe(1)",
    )
    require(
        f"{base}/buildcraft/silicon/container/ContainerAdvancedCraftingTable.java",
        "BUTTON_PREVIOUS_RECIPE",
        "BUTTON_NEXT_RECIPE",
        "tile.cycleRecipe(-1)",
        "tile.cycleRecipe(1)",
    )
    require(
        f"source-platforms/{platform}/src/gametest/java/buildcraft/gametest/WorkbenchRollbackGameTests.java",
        "failedGridClearNeverOverwritesTransientItems",
        "crafting.setItem(0, new ItemStack(Items.DIAMOND))",
        "!crafted",
    )

require(
    "source-families/legacy/src/main/java/buildcraft/builders/block/BlockConstructionMarker.java",
    "allowed to continue to their own Item#useOn implementation",
    "return InteractionResult.PASS;",
)
require(
    "source-families/legacy/src/main/java/buildcraft/factory/block/BlockFloodGate.java",
    "A wrench interaction belongs to the Flood Gate",
    "return InteractionResult.SUCCESS;",
)

for rel in (
    "version-src/1.19.2-forge/src/main/java/buildcraft/factory/gui/GuiAutoCraftItems.java",
    "version-src/1.20.1-forge/src/main/java/buildcraft/factory/gui/GuiAutoCraftItems.java",
    "source-families/modern/src/main/java/buildcraft/factory/gui/GuiAutoCraftItems.java",
    "version-src/1.19.2-forge/src/main/java/buildcraft/silicon/gui/GuiAdvancedCraftingTable.java",
    "version-src/1.20.1-forge/src/main/java/buildcraft/silicon/gui/GuiAdvancedCraftingTable.java",
    "source-families/modern/src/main/java/buildcraft/silicon/gui/GuiAdvancedCraftingTable.java",
):
    require(rel, "slot instanceof SlotDisplay", "handleInventoryButtonClick")

# The old oil biomes were registered only on 1.19.2 and never participated in
# the current biome-modifier worldgen path. Keep the useful oil eligibility tag,
# but reject the dead registry/API surface everywhere.
for root in ("source-families", "source-platforms", "source-shared", "version-src"):
    for path in (ROOT / root).rglob("*"):
        if not path.is_file() or path.suffix not in {".java", ".json"}:
            continue
        source = path.read_text(encoding="utf-8", errors="ignore")
        for obsolete in ("oil_desert", "oil_deep_ocean", "OIL_DESERT_KEY", "OIL_DEEP_OCEAN_KEY"):
            if obsolete in source:
                errors.append(f"{path.relative_to(ROOT)}: obsolete custom oil biome reference {obsolete!r}")

for rel in (
    "source-families/legacy/src/main/resources/data/forge/tags/worldgen/biome/is_desert.json",
    "source-families/legacy/src/main/resources/data/forge/tags/worldgen/biome/is_sandy.json",
):
    if (ROOT / rel).exists():
        errors.append(f"{rel}: obsolete tag extension for removed oil biome still exists")

require(
    "version-src/1.19.2-forge/src/main/java/buildcraft/energy/BCEnergyWorldGen.java",
    "BCEnergyBiomeModifiers.register(modEventBus)",
    "FEATURE_REGISTER.register(modEventBus)",
)
require(
    "source-shared/src/main/resources/data/buildcraftenergy/tags/worldgen/biome/is_oil_biome.json",
    "minecraft:desert",
    "minecraft:deep_ocean",
    "minecraft:deep_cold_ocean",
)

if errors:
    for error in errors:
        print("ERROR:", error)
    sys.exit(1)

print("P3 regression guards OK")
print(" - Workbench rollback cannot overwrite transient crafting items")
print(" - conflicting crafting outputs have a persistent GUI selector")
print(" - Construction Marker and Flood Gate interaction parity is guarded")
print(" - dead custom oil biomes and their legacy Forge tag hooks are removed")
