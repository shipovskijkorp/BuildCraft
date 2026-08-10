/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon;

import buildcraft.lib.recipe.AssemblyRecipe;
import buildcraft.lib.recipe.AssemblyRecipeBasic;
import buildcraft.silicon.recipe.FacadeAssemblyRecipes;
import buildcraft.silicon.recipe.FacadeSwapRecipe;
import buildcraft.silicon.recipe.GateLogicChangeRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class BCSiliconRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES =
        DeferredRegister.create(Registries.RECIPE_TYPE, BCSilicon.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, BCSilicon.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<AssemblyRecipeBasic>> ASSEMBLY_TYPE = TYPES.register(
        "assembly", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(BCSilicon.MODID, "assembly")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AssemblyRecipe>> ASSEMBLY_SERIALIZER =
        SERIALIZERS.register("assembly", AssemblyRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<GateLogicChangeRecipe>> GATE_CHANGE_SERIALIZER =
        SERIALIZERS.register("gate_logic_change", () -> new SimpleCraftingRecipeSerializer<>(GateLogicChangeRecipe::new));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FacadeAssemblyRecipes>> FACADE_SERIALIZER =
        SERIALIZERS.register("facade", FacadeAssemblyRecipes.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FacadeSwapRecipe>> FACADE_SWAP_SERIALIZER =
        SERIALIZERS.register("facade_swap", () -> FacadeSwapRecipe.SERIALIZER);

    private BCSiliconRecipes() {
    }

    public static void preInit(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        SERIALIZERS.register(modEventBus);
    }
}
