/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport;

import buildcraft.api.transport.pipe.PipeApiClient;
import buildcraft.transport.client.PipeRegistryClient;
import buildcraft.transport.client.model.ModelPipe;
import buildcraft.transport.client.model.PipeBaseModelGenStandard;
import buildcraft.transport.client.model.PipeModelCacheAll;
import buildcraft.transport.client.render.PipeFlowRendererPower;
import buildcraft.transport.net.PipeItemMessageQueue;
import buildcraft.transport.wire.WorldSavedDataWireSystems;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.ModelEvent.BakingCompleted;
import net.minecraftforge.client.event.ModelEvent.ModifyBakingResult;
import net.minecraftforge.client.event.ModelEvent.RegisterAdditional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class BCTransportEventDist {

    @Mod.EventBusSubscriber(modid = BCTransport.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
    	public static final ResourceLocation TRUNK_LIGHT = new ResourceLocation("buildcraftcore:blocks/engine/trunk_light");
    	public static final ResourceLocation CHAMBER = new ResourceLocation("buildcraftcore:blocks/engine/chamber_base");
    	
        private static void ensureClientRegistry() {
            if (PipeApiClient.registry == null) {
                PipeApiClient.registry = PipeRegistryClient.INSTANCE;
            }
            BCTransportModels.init();
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            ensureClientRegistry();
            BCTransportClientGuis.clientInit(event);
        }
        
        @SubscribeEvent
        public static void registryRender(EntityRenderersEvent.RegisterRenderers e) {
        	BCTransportModels.onBlockEntityRender(e);
        }
        
        @SubscribeEvent
        public static void onBlockColor(RegisterColorHandlersEvent.Block event) {
        	BCTransportModels.onBlockColor(event);
        }
        
        @SubscribeEvent
        public static void onModelBakePre(RegisterAdditional event) {
            ensureClientRegistry();
            BCTransportModels.onModelBakePre(event);
        }
        
        @SubscribeEvent
        public static void onModelBake(ModifyBakingResult event) {
            ensureClientRegistry();
            BCTransportModels.onModelBake(event);
        }

        @SubscribeEvent
        public static void onModelBakeComplete(BakingCompleted event) {
            PipeBaseModelGenStandard.loadSpritesCache();
            PipeModelCacheAll.clearModels();
            ModelPipe.clearTextureCache();
            PipeFlowRendererPower.clearTextureCache();
            BCTransportModels.onModelBakeComplete();
        }
        
        
    }
    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (!event.level.isClientSide && event.level.getServer() != null) {
            WorldSavedDataWireSystems.get(event.level).tick();
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        PipeItemMessageQueue.serverTick();
    }

    @SubscribeEvent
    public static void onChunkWatch(ChunkWatchEvent.Watch event) {
        WorldSavedDataWireSystems.get(event.getLevel()).changedPlayers.add(event.getPlayer());
    }
}
