/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package ct.buildcraft.lib;

import ct.buildcraft.api.core.BuildCraftAPI;
import ct.buildcraft.api.crops.CropManager;
import ct.buildcraft.api.fuels.BuildcraftFuelRegistry;
import ct.buildcraft.api.recipes.BuildcraftRecipeRegistry;
import ct.buildcraft.api.transport.pipe.PipeApi;
import ct.buildcraft.lib.crops.CropHandlerPlantable;
import ct.buildcraft.lib.crops.CropHandlerReeds;
import ct.buildcraft.lib.fluid.CoolantRegistry;
import ct.buildcraft.lib.fluid.FuelRegistry;
import ct.buildcraft.lib.misc.FakePlayerProvider;
import ct.buildcraft.lib.recipe.IntegrationRecipeRegistry;
import ct.buildcraft.lib.recipe.RefineryRecipeRegistry;
import ct.buildcraft.lib.registry.PluggableRegistry;

public class BCLibRegistries {
    /**
     * Initializes API registries that can be used by other BuildCraft modules while Forge is still constructing mods.
     * <p>
     * BuildCraft is distributed as one jar, but Forge still constructs buildcraftlib, buildcrafttransport,
     * buildcraftsilicon, etc. as separate mods. In large modpacks the construction order can differ, so modules must
     * not assume that the buildcraftlib constructor has already populated static API registries.
     */
    public static synchronized void initApiRegistries() {
        if (PipeApi.pluggableRegistry == null) {
            PipeApi.pluggableRegistry = PluggableRegistry.INSTANCE;
        }
    }

    public static void fmlPreInit() {
        BuildcraftRecipeRegistry.integrationRecipes = IntegrationRecipeRegistry.INSTANCE;
        BuildcraftRecipeRegistry.refineryRecipes = RefineryRecipeRegistry.INSTANCE;
        BuildcraftFuelRegistry.fuel = FuelRegistry.INSTANCE;
        BuildcraftFuelRegistry.coolant = CoolantRegistry.INSTANCE;
        BuildCraftAPI.fakePlayerProvider = FakePlayerProvider.INSTANCE;
        initApiRegistries();

//        ReloadableRegistryManager dataManager = ReloadableRegistryManager.DATA_PACKS;
//        BuildCraftRegistryManager.managerDataPacks = dataManager;
//        dataManager.registerRegistry(GuideBookRegistry.INSTANCE);

        CropManager.setDefaultHandler(CropHandlerPlantable.INSTANCE);
        CropManager.registerHandler(CropHandlerReeds.INSTANCE);
    }

    public static void fmlInit() {}
}
