/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;

import buildcraft.lib.internal.tiles.IDebuggable;
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
import buildcraft.lib.misc.ItemStackUtil;
import buildcraft.lib.misc.MessageUtil;
import buildcraft.lib.misc.SpriteUtil;
import buildcraft.lib.misc.data.ModelVariableData;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ModelEvent.BakingCompleted;
import net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;


public class BCLibEventDist {
	
	@EventBusSubscriber(modid = BCLib.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
        public static void textureStitchPost(TextureAtlasStitchedEvent event) {
	    	if (InventoryMenu.BLOCK_ATLAS.equals(event.getAtlas().location())) {
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
                // Variable models are reparsed in this pass, so old expression-node arrays are no longer valid.
                ModelVariableData.onModelBake();
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

	@SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void renderWorldLast(RenderLevelStageEvent event) {
    	if(event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
    		return ;
    	}
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        PoseStack pose = new PoseStack();
        pose.mulPose(new Matrix4f(event.getModelViewMatrix()));
        Matrix4f matrix = new Matrix4f(event.getProjectionMatrix());
        float partialTicks = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        
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

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onConnectToServer(ClientPlayerNetworkEvent.LoggingIn event) {
        Minecraft mc = Minecraft.getInstance();
        ItemStackUtil.setClientRegistryProvider(mc.level == null ? null : mc.level.registryAccess());
        MarkerCache.clearClientCaches();
        MessageMarkerClientHandler.clearQueuedMessages();
        BuildCraftObjectCaches.onClientJoinServer();
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onDisconnectFromServer(ClientPlayerNetworkEvent.LoggingOut event) {
        ItemStackUtil.setClientRegistryProvider(null);
        MarkerCache.clearClientCaches();
        MessageMarkerClientHandler.clearQueuedMessages();
    }


    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        BCAdvDebugging.INSTANCE.onServerPostTick();
        MessageUtil.postServerTick();
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void clientTick(ClientTickEvent.Post event) {
        BuildCraftObjectCaches.onClientTick();
        MessageUtil.postClientTick();
        MessageMarkerClientHandler.flushQueuedMessages();
        Minecraft mc = Minecraft.getInstance();
        ItemStackUtil.setClientRegistryProvider(mc.level == null ? null : mc.level.registryAccess());
        LocalPlayer player = mc.player;
        if (player != null && ItemDebugger.isShowDebugInfo(player)) {
            HitResult mouseOver = mc.hitResult;
            if (mouseOver != null) {
                IDebuggable debuggable = ClientDebuggables.getDebuggableObject(mouseOver);
                if (debuggable instanceof BlockEntity tile && mouseOver instanceof BlockHitResult blockHit) {
                    MessageManager.sendToServer(new MessageDebugRequest(tile.getBlockPos(), blockHit.getDirection()));
                } else if (debuggable instanceof Entity) {
                    // TODO: Add entity debug-info request/response support.
                }
            }
        }
    }
}
