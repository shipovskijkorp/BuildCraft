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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.ModelEvent.BakingCompleted;
import net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult;
import net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public class BCTransportEventDist {

    @EventBusSubscriber(modid = BCTransport.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        public static final ResourceLocation TRUNK_LIGHT = ResourceLocation.parse("buildcraftcore:blocks/engine/trunk_light");
        public static final ResourceLocation CHAMBER = ResourceLocation.parse("buildcraftcore:blocks/engine/chamber_base");
    	
        private static void ensureClientRegistry() {
            if (PipeApiClient.registry == null) {
                PipeApiClient.registry = PipeRegistryClient.INSTANCE;
            }
            BCTransportModels.init();
        }

        @SubscribeEvent
        public static void registerMenuScreens(RegisterMenuScreensEvent event) {
            BCTransportClientGuis.clientInit(event);
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            ensureClientRegistry();

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
            clearAtlasDependentPipeCaches();
            BCTransportModels.onModelBakeComplete();
        }

        @SubscribeEvent
        public static void onTextureAtlasStitched(TextureAtlasStitchedEvent event) {
            if (InventoryMenu.BLOCK_ATLAS.equals(event.getAtlas().location())) {
                PipeBaseModelGenStandard.loadSpritesCache(event.getAtlas());
                clearAtlasDependentPipeCaches();
            }
        }

        private static void clearAtlasDependentPipeCaches() {
            PipeModelCacheAll.clearModels();
            ModelPipe.clearTextureCache();
            PipeFlowRendererPower.clearTextureCache();
        }
        
        
    }
    @SubscribeEvent
    public static void onWorldTick(LevelTickEvent.Pre event) {
        tickWorld(event.getLevel());
    }

    @SubscribeEvent
    public static void onWorldTick(LevelTickEvent.Post event) {
        tickWorld(event.getLevel());
    }

    private static void tickWorld(net.minecraft.world.level.Level level) {
        if (!level.isClientSide && level.getServer() != null) {
            WorldSavedDataWireSystems.get(level).tick();
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        PipeItemMessageQueue.serverTick();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        PipeItemMessageQueue.serverTick();
    }

    @SubscribeEvent
    public static void onChunkWatch(ChunkWatchEvent.Watch event) {
        WorldSavedDataWireSystems.get(event.getLevel()).changedPlayers.add(event.getPlayer());
    }
}
