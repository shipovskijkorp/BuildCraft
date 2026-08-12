/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib;

import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.v2.BuildCraftApi;
import buildcraft.api.v2.BuildCraftServices;
import buildcraft.api.v2.crops.CropService;
import buildcraft.lib.internal.api.v2.BuildCraftApiRuntime;
import buildcraft.lib.internal.api.v2.BuiltInApi2Content;
import buildcraft.lib.crops.CropHandlerPlantable;
import buildcraft.lib.crops.CropHandlerReeds;
import buildcraft.lib.registry.PluggableRegistry;

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
        BuildCraftApiRuntime.bootstrap();
        BuiltInApi2Content.register();
        initApiRegistries();

//        ReloadableRegistryManager dataManager = ReloadableRegistryManager.DATA_PACKS;
//        BuildCraftRegistryManager.managerDataPacks = dataManager;
//        dataManager.registerRegistry(GuideBookRegistry.INSTANCE);

        CropService crops = BuildCraftApi.service(BuildCraftServices.CROPS);
        crops.register(new net.minecraft.resources.ResourceLocation("buildcraft", "plantable"), -1000, CropHandlerPlantable.INSTANCE);
        crops.register(new net.minecraft.resources.ResourceLocation("buildcraft", "reeds"), 100, CropHandlerReeds.INSTANCE);
    }

    public static void fmlInit() {}
}
