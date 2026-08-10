/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy;

import java.util.concurrent.CompletableFuture;

import buildcraft.api.BCModules;
import buildcraft.api.fuels.BuildcraftFuelRegistry;
import buildcraft.api.mj.MjAPI;
import buildcraft.lib.recipe.RefineryRecipeRegistry;
import buildcraft.api.recipes.IRefineryRecipeManager.IDistillationRecipe;
import buildcraft.core.BCCoreItems;
import buildcraft.lib.fluid.BCFluid;
import buildcraft.lib.misc.MathUtil;
import net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

public final class BCEnergyRecipes {
    private static final int TIME_BASE = 240_000;

    private BCEnergyRecipes() {
    }

    public static void init() {
        BuildcraftFuelRegistry.coolant.addCoolant(Fluids.WATER, 0.0023f);
        BuildcraftFuelRegistry.coolant.addSolidCoolant(
            new ItemStack(Blocks.ICE), new FluidStack(Fluids.WATER, 1000), 1.5f
        );
        BuildcraftFuelRegistry.coolant.addSolidCoolant(
            new ItemStack(Blocks.PACKED_ICE), new FluidStack(Fluids.WATER, 1000), 2f
        );

        final int oil = 8;
        final int gas = 16;
        final int light = 4;
        final int dense = 2;
        final int residue = 1;
        final int gasLight = 10;
        final int lightDense = 5;
        final int denseResidue = 2;
        final int lightDenseResidue = 3;
        final int gasLightDense = 8;

        addFuel(BCEnergyFluids.fuelGaseous, gas, 8, 4);
        addFuel(BCEnergyFluids.fuelLight, light, 6, 6);
        addFuel(BCEnergyFluids.fuelDense, dense, 4, 12);
        addFuel(BCEnergyFluids.fuelMixedLight, gasLight, 3, 5);
        addFuel(BCEnergyFluids.fuelMixedHeavy, lightDense, 5, 8);
        addDirtyFuel(BCEnergyFluids.oilDense, denseResidue, 4, 4);
        addFuel(BCEnergyFluids.oilDistilled, gasLightDense, 1, 5);
        addDirtyFuel(BCEnergyFluids.oilHeavy, lightDenseResidue, 2, 4);
        addDirtyFuel(BCEnergyFluids.crudeOil, oil, 3, 4);

        if (!BCModules.FACTORY.isLoaded()) {
            return;
        }

        FluidStack[] gasLightDenseResidue = createFluidStack(BCEnergyFluids.crudeOil, oil);
        FluidStack[] gasLightDenseStacks = createFluidStack(BCEnergyFluids.oilDistilled, gasLightDense);
        FluidStack[] gasLightStacks = createFluidStack(BCEnergyFluids.fuelMixedLight, gasLight);
        FluidStack[] gasStacks = createFluidStack(BCEnergyFluids.fuelGaseous, gas);
        FluidStack[] lightDenseResidueStacks = createFluidStack(BCEnergyFluids.oilHeavy, lightDenseResidue);
        FluidStack[] lightDenseStacks = createFluidStack(BCEnergyFluids.fuelMixedHeavy, lightDense);
        FluidStack[] lightStacks = createFluidStack(BCEnergyFluids.fuelLight, light);
        FluidStack[] denseResidueStacks = createFluidStack(BCEnergyFluids.oilDense, denseResidue);
        FluidStack[] denseStacks = createFluidStack(BCEnergyFluids.fuelDense, dense);
        FluidStack[] residueStacks = createFluidStack(BCEnergyFluids.oilResidue, residue);

        addDistillation(gasLightDenseResidue, gasStacks, lightDenseResidueStacks, 0, 32 * MjAPI.MJ);
        addDistillation(gasLightDenseResidue, gasLightStacks, denseResidueStacks, 1, 16 * MjAPI.MJ);
        addDistillation(gasLightDenseResidue, gasLightDenseStacks, residueStacks, 2, 12 * MjAPI.MJ);
        addDistillation(gasLightDenseStacks, gasStacks, lightDenseStacks, 0, 24 * MjAPI.MJ);
        addDistillation(gasLightDenseStacks, gasLightStacks, denseStacks, 1, 16 * MjAPI.MJ);
        addDistillation(gasLightStacks, gasStacks, lightStacks, 0, 24 * MjAPI.MJ);
        addDistillation(lightDenseResidueStacks, lightStacks, denseResidueStacks, 1, 16 * MjAPI.MJ);
        addDistillation(lightDenseResidueStacks, lightDenseStacks, residueStacks, 2, 12 * MjAPI.MJ);
        addDistillation(lightDenseStacks, lightStacks, denseStacks, 1, 16 * MjAPI.MJ);
        addDistillation(denseResidueStacks, denseStacks, residueStacks, 2, 12 * MjAPI.MJ);

        addHeatExchange(BCEnergyFluids.crudeOil);
        addHeatExchange(BCEnergyFluids.oilDistilled);
        addHeatExchange(BCEnergyFluids.oilHeavy);
        addHeatExchange(BCEnergyFluids.oilDense);
        addHeatExchange(BCEnergyFluids.fuelMixedLight);
        addHeatExchange(BCEnergyFluids.fuelMixedHeavy);
        addHeatExchange(BCEnergyFluids.fuelGaseous);
        addHeatExchange(BCEnergyFluids.fuelLight);
        addHeatExchange(BCEnergyFluids.fuelDense);
        addHeatExchange(BCEnergyFluids.oilResidue);

        RefineryRecipeRegistry.INSTANCE.addHeatableRecipe(new FluidStack(Fluids.WATER, 10), null, 0, 1);
        RefineryRecipeRegistry.INSTANCE.addCoolableRecipe(new FluidStack(Fluids.LAVA, 5), null, 4, 2);
    }

    private static FluidStack[] createFluidStack(Fluid[] fluids, int amount) {
        FluidStack[] result = new FluidStack[fluids.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = new FluidStack(fluids[i], amount);
        }
        return result;
    }

    private static Fluid getFirstOrNull(Fluid[] fluids) {
        return fluids == null || fluids.length == 0 ? null : fluids[0];
    }

    private static void addFuel(Fluid[] input, int amountDifference, int multiplier, int boostOverFour) {
        Fluid fuel = getFirstOrNull(input);
        if (fuel == null) {
            return;
        }
        long powerPerCycle = multiplier * MjAPI.MJ;
        int totalTime = TIME_BASE * boostOverFour / 4 / multiplier / amountDifference;
        BuildcraftFuelRegistry.fuel.addFuel(fuel, powerPerCycle, totalTime);
    }

    private static void addDirtyFuel(Fluid[] input, int amountDifference, int multiplier, int boostOverFour) {
        Fluid fuel = getFirstOrNull(input);
        if (fuel == null) {
            return;
        }
        long powerPerCycle = multiplier * MjAPI.MJ;
        int totalTime = TIME_BASE * boostOverFour / 4 / multiplier / amountDifference;
        Fluid residue = getFirstOrNull(BCEnergyFluids.oilResidue);
        if (residue == null) {
            BuildcraftFuelRegistry.fuel.addFuel(fuel, powerPerCycle, totalTime);
        } else {
            BuildcraftFuelRegistry.fuel.addDirtyFuel(
                fuel, powerPerCycle, totalTime, new FluidStack(residue, 1000 / amountDifference)
            );
        }
    }

    private static void addDistillation(
        FluidStack[] input, FluidStack[] outputGas, FluidStack[] outputLiquid, int heat, long mjCost
    ) {
        FluidStack inputStack = input[heat];
        FluidStack gasStack = outputGas[heat];
        FluidStack liquidStack = outputLiquid[heat];
        IDistillationRecipe existing =
            RefineryRecipeRegistry.INSTANCE.getDistillationRegistry().getRecipeForInput(inputStack);
        if (existing != null) {
            throw new IllegalStateException(
                "Already added distillation recipe for " + inputStack.getFluid().getFluidType().getDescriptionId()
            );
        }

        int hcf = MathUtil.findHighestCommonFactor(inputStack.getAmount(), gasStack.getAmount());
        hcf = MathUtil.findHighestCommonFactor(hcf, liquidStack.getAmount());
        if (hcf > 1) {
            inputStack = inputStack.copy();
            gasStack = gasStack.copy();
            liquidStack = liquidStack.copy();
            inputStack.setAmount(inputStack.getAmount() / hcf);
            gasStack.setAmount(gasStack.getAmount() / hcf);
            liquidStack.setAmount(liquidStack.getAmount() / hcf);
            mjCost /= hcf;
        }
        RefineryRecipeRegistry.INSTANCE.addDistillationRecipe(inputStack, gasStack, liquidStack, mjCost);
    }

    private static void addHeatExchange(BCFluid[] fluids) {
        for (int i = 0; i < fluids.length - 1; i++) {
            BCFluid cool = fluids[i];
            BCFluid hot = fluids[i + 1];
            FluidStack coolStack = new FluidStack(cool, 10);
            FluidStack hotStack = new FluidStack(hot, 10);
            int coolHeat = cool.getHeatValue();
            int hotHeat = hot.getHeatValue();
            RefineryRecipeRegistry.INSTANCE.addHeatableRecipe(coolStack, hotStack, coolHeat, hotHeat);
            RefineryRecipeRegistry.INSTANCE.addCoolableRecipe(hotStack, coolStack, hotHeat, coolHeat);
        }
    }

    public static final class BCEnergyRecipeProvider extends RecipeProvider {
        public BCEnergyRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected void buildRecipes(RecipeOutput writer) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BCEnergyBlocks.ENGINE_STONE_ITEM.get())
                .pattern("www")
                .pattern(" g ")
                .pattern("GpG")
                .define('w', ItemTags.STONE_TOOL_MATERIALS)
                .define('g', Items.GLASS)
                .define('G', BCCoreItems.GEAR_STONE.get())
                .define('p', Blocks.PISTON)
                .unlockedBy(
                    "has_" + BCCoreItems.GEAR_STONE.getId().getPath(),
                    TriggerInstance.hasItems(BCCoreItems.GEAR_STONE.get())
                )
                .save(writer, ResourceLocation.fromNamespaceAndPath(BCEnergy.MODID, "stirling_engine"));

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BCEnergyBlocks.ENGINE_IRON_ITEM.get())
                .pattern("www")
                .pattern(" g ")
                .pattern("GpG")
                .define('w', Items.IRON_INGOT)
                .define('g', Items.GLASS)
                .define('G', BCCoreItems.GEAR_IRON.get())
                .define('p', Blocks.PISTON)
                .unlockedBy(
                    "has_" + BCCoreItems.GEAR_IRON.getId().getPath(),
                    TriggerInstance.hasItems(BCCoreItems.GEAR_IRON.get())
                )
                .save(writer, ResourceLocation.fromNamespaceAndPath(BCEnergy.MODID, "combustion_engine"));
        }
    }
}
