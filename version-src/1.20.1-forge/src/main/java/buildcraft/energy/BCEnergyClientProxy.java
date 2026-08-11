/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 */
package buildcraft.energy;

import buildcraft.core.client.render.RenderEngine_BC8;
import buildcraft.energy.client.gui.GuiDynamoMJ;
import buildcraft.energy.client.gui.GuiEngineFE;
import buildcraft.energy.client.gui.GuiEngineIron_BC8;
import buildcraft.energy.client.gui.GuiEngineStone_BC8;
import buildcraft.energy.client.render.RenderDynamoMJ;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = BCEnergy.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public abstract class BCEnergyClientProxy {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        BCEnergySprites.init();
        event.enqueueWork(() -> {
            MenuScreens.register(BCEnergyGuis.MENU_STONE.get(), GuiEngineStone_BC8::new);
            MenuScreens.register(BCEnergyGuis.MENU_IRON.get(), GuiEngineIron_BC8::new);
            MenuScreens.register(BCEnergyGuis.MENU_FE.get(), GuiEngineFE::new);
            MenuScreens.register(BCEnergyGuis.MENU_DYNAMO_MJ.get(), GuiDynamoMJ::new);
        });
    }

    @SubscribeEvent
    public static void registryRender(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BCEnergyBlocks.ENGINE_IRON_TILE_BC8.get(), RenderEngine_BC8::new);
        event.registerBlockEntityRenderer(BCEnergyBlocks.ENGINE_STONE_TILE_BC8.get(), RenderEngine_BC8::new);
        event.registerBlockEntityRenderer(BCEnergyBlocks.ENGINE_FE_TILE_BC8.get(), RenderEngine_BC8::new);
        event.registerBlockEntityRenderer(BCEnergyBlocks.DYNAMO_MJ_TILE.get(), RenderDynamoMJ::new);
    }
}
