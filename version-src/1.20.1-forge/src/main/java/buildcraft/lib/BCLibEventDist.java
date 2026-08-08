/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;

import buildcraft.api.tiles.IDebuggable;
import buildcraft.lib.client.model.ModelHolderRegistry;
import buildcraft.lib.client.model.json.VariablePartLed;
import buildcraft.lib.client.reload.LibConfigChangeListener;
import buildcraft.lib.client.reload.ReloadManager;
import buildcraft.lib.client.render.DetachedRenderer;
import buildcraft.lib.client.render.laser.LaserRenderer_BC8;
import buildcraft.lib.client.render.DetachedRenderer.RenderMatrixType;
import buildcraft.lib.client.render.MarkerRenderer;
import buildcraft.lib.client.render.fluid.FluidRenderer;
import buildcraft.lib.client.sprite.SpriteHolderRegistry;
import buildcraft.lib.debug.BCAdvDebugging;
import buildcraft.lib.debug.ClientDebuggables;
import buildcraft.lib.debug.DebugRenderHelper;
import buildcraft.lib.item.ItemDebugger;
import buildcraft.lib.marker.MarkerCache;
import buildcraft.lib.misc.FakePlayerProvider;
import buildcraft.lib.misc.MessageUtil;
import buildcraft.lib.misc.data.ModelVariableData;
import buildcraft.lib.misc.SpriteUtil;
import buildcraft.lib.net.MessageDebugRequest;
import buildcraft.lib.net.MessageDebugResponse;
import buildcraft.lib.net.MessageManager;
import buildcraft.lib.net.MessageMarker;
import buildcraft.lib.net.MessageMarkerClientHandler;
import buildcraft.lib.net.cache.BuildCraftObjectCaches;
import buildcraft.lib.net.cache.MessageObjectCacheResponse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ModelEvent.BakingCompleted;
import net.minecraftforge.client.event.ModelEvent.RegisterAdditional;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;


public class BCLibEventDist {
	
	@Mod.EventBusSubscriber(modid = BCLib.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
	public static class Client {
		
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
/*            ReloadableRegistryManager manager = ReloadableRegistryManager.RESOURCE_PACKS;
            BuildCraftRegistryManager.managerResourcePacks = manager;
            manager.registerRegistry(GuidePageRegistry.INSTANCE);*/

            DetachedRenderer.INSTANCE.addRenderer(RenderMatrixType.FROM_WORLD_ORIGIN, MarkerRenderer.INSTANCE);
            // various sprite registers
            BCLibSprites.fmlPreInitClient();
            SpriteHolderRegistry.bootstrapBuiltinHolders();
            BCLibConfig.configChangeListeners.add(LibConfigChangeListener.INSTANCE);

            MessageManager.setHandler(MessageMarker.class, MessageMarker.HANDLER, Dist.CLIENT);
            MessageManager.setHandler(MessageObjectCacheResponse.class, MessageObjectCacheResponse.HANDLER, Dist.CLIENT);
            MessageManager.setHandler(MessageDebugResponse.class, MessageDebugResponse.HANDLER, Dist.CLIENT);
        }
        
        @SubscribeEvent
        public static void textureStitchPost(TextureStitchEvent.Post event) {
	    	if (InventoryMenu.BLOCK_ATLAS.equals(event.getAtlas().location())) {
                // Variable models are reparsed below, so expression nodes from the old JSON generation are stale.
                ModelVariableData.onModelBake();
                ModelHolderRegistry.reloadVariableModels();
	            SpriteHolderRegistry.onTextureStitchPost(event);
            SpriteUtil.clearAtlasCache();
            DebugRenderHelper.clearTextureCache();

                // Laser vertex buffers contain absolute UV coordinates from the atlas.
                // Rebuild them after every stitch, including extra reloads started by other mods.
                LaserRenderer_BC8.clearModels();
	            FluidRenderer.onTextureStitchPost(event);
                VariablePartLed.onTextureStitchPost(event);
	        }
	    }
	    
	    @SubscribeEvent
	    public static void preModelBake(RegisterAdditional event) {
	    	ModelHolderRegistry.preModelBake(event);
	    }
	
	    @SubscribeEvent
	    public static void onModelBake(BakingCompleted event) {
	        SpriteHolderRegistry.exportTextureMap();
	        LaserRenderer_BC8.clearModels();
	        ModelHolderRegistry.onModelBake(event);
	    }

	    
	}
	/*	    @SubscribeEvent
    public static void renderWorldLast(RenderLevelLastEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        float partialTicks = event.getPartialTicks();

//        DetachedRenderer.INSTANCE.renderWorldLastEvent(player, partialTicks);
    }
*/
	@SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void renderWorldLast(RenderLevelStageEvent event) {
    	if(event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
    		return ;
    	}
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        PoseStack pose = event.getPoseStack();
        Matrix4f matrix = new Matrix4f(RenderSystem.getProjectionMatrix());
        float partialTicks = event.getPartialTick();
        
        LaserRenderer_BC8.setupLaserRenderState();
        DetachedRenderer.INSTANCE.renderWorldLastEvent(pose, matrix, player, partialTicks);
    }

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayer) {
            ServerPlayer playerMP = (ServerPlayer) entity;
            // Delay sending join messages as it makes it work when in single-player.
            // Send a few sync passes because marker messages can otherwise arrive before the client world exists
            // during integrated-server login; the client queues those messages too, but these extra passes make
            // reconnecting to an already loaded marker network deterministic.
            MessageUtil.doDelayedServer(1, () -> MarkerCache.onPlayerJoinLevel(playerMP));
            MessageUtil.doDelayedServer(5, () -> MarkerCache.onPlayerJoinLevel(playerMP));
            MessageUtil.doDelayedServer(20, () -> MarkerCache.onPlayerJoinLevel(playerMP));
            MessageUtil.doDelayedServer(60, () -> MarkerCache.onPlayerJoinLevel(playerMP));
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        MarkerCache.onLevelUnload(event.getLevel());
        if (event.getLevel() instanceof ServerLevel) {
            FakePlayerProvider.INSTANCE.unloadWorld((ServerLevel) event.getLevel());
        }
    }
/*
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onReloadFinish(EventBuildCraftReload.FinishLoad event) {
        // Note: when you need to add server-side listeners the client listeners need to be moved to BCLibProxy
        GuideManager.INSTANCE.onRegistryReload(event);
    }
*/
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onConnectToServer(ClientPlayerNetworkEvent.LoggingIn event) {
        MarkerCache.clearClientCaches();
        MessageMarkerClientHandler.clearQueuedMessages();
        BuildCraftObjectCaches.onClientJoinServer();
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onDisconnectFromServer(ClientPlayerNetworkEvent.LoggingOut event) {
        MarkerCache.clearClientCaches();
        MessageMarkerClientHandler.clearQueuedMessages();
    }


    @SubscribeEvent
    public static void serverTick(ServerTickEvent event) {
        if (event.phase == Phase.END) {
            BCAdvDebugging.INSTANCE.onServerPostTick();
            MessageUtil.postServerTick();
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void clientTick(ClientTickEvent event) {
        if (event.phase == Phase.END) {
            BuildCraftObjectCaches.onClientTick();
            MessageUtil.postClientTick();
            MessageMarkerClientHandler.flushQueuedMessages();
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player != null && ItemDebugger.isShowDebugInfo(player)) {
                HitResult mouseOver = mc.hitResult;
                if (mouseOver != null) {
                    IDebuggable debuggable = ClientDebuggables.getDebuggableObject(mouseOver);
                    if (debuggable instanceof BlockEntity tile && mouseOver instanceof BlockHitResult blockHit) {
                        MessageManager.sendToServer(new MessageDebugRequest(tile.getBlockPos(), blockHit.getDirection()));
                    } else if (debuggable instanceof Entity) {
                        // TODO: Support entities!
                    }
                }
            }
        }
    }
}
