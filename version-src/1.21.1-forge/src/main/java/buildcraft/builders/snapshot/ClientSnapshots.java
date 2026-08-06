/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.snapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;

import buildcraft.lib.net.MessageManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

public enum ClientSnapshots {
    INSTANCE;

    private static final int COMBINED_LIGHT = 0x00F0_00F0;

    private final List<Snapshot> snapshots = new ArrayList<>();
    private final List<Snapshot.Key> pending = new ArrayList<>();
    private final Map<Snapshot.Key, FakeWorld> worlds = new HashMap<>();

    public Snapshot getSnapshot(Snapshot.Key key) {
        Snapshot found = snapshots.stream().filter(snapshot -> snapshot.key.equals(key)).findFirst().orElse(null);
        if (found == null && !pending.contains(key)) {
            pending.add(key);
            MessageManager.sendToServer(new MessageSnapshotRequest(key));
        }
        return found;
    }

    public void onSnapshotReceived(Snapshot snapshot) {
        pending.remove(snapshot.key);
        snapshots.removeIf(existing -> existing.key.equals(snapshot.key));
        snapshots.add(snapshot);
        worlds.remove(snapshot.key);
    }

    @OnlyIn(Dist.CLIENT)
    public void renderSnapshot(PoseStack ignoredPose, Snapshot.Header header, int offsetX, int offsetY, int sizeX, int sizeY) {
        if (header == null) {
            return;
        }
        Snapshot snapshot = getSnapshot(header.key);
        if (snapshot != null) {
            renderSnapshot(ignoredPose, snapshot, offsetX, offsetY, sizeX, sizeY);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void renderSnapshot(PoseStack ignoredPose, Snapshot snapshot, int offsetX, int offsetY, int sizeX, int sizeY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || snapshot == null || snapshot.size == null) {
            return;
        }

        FakeWorld world = worlds.computeIfAbsent(snapshot.key, key -> {
            FakeWorld fakeWorld = new FakeWorld(minecraft.level);
            fakeWorld.uploadSnapshot(snapshot);
            return fakeWorld;
        });

        float partialTick = minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        double guiScale = minecraft.getWindow().getGuiScale();
        int viewportX = (int) (offsetX * guiScale);
        int viewportY = (int) (minecraft.getWindow().getHeight() - (sizeY + offsetY) * guiScale);
        int viewportWidth = Math.max(1, (int) (sizeX * guiScale));
        int viewportHeight = Math.max(1, (int) (sizeY * guiScale));

        RenderSystem.backupProjectionMatrix();
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        PoseStack snapshotPose = new PoseStack();
        try {
            Matrix4f projection = new Matrix4f().identity()
                .perspective((float) Math.toRadians(70.0), (float) sizeX / Math.max(1, sizeY), 0.1F, 1000.0F);
            RenderSystem.setProjectionMatrix(projection, VertexSorting.ORTHOGRAPHIC_Z);

            RenderSystem.enableScissor(
                viewportX,
                viewportY,
                viewportWidth,
                viewportHeight
            );
            RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            RenderSystem.disableScissor();
            RenderSystem.viewport(viewportX, viewportY, viewportWidth, viewportHeight);

            modelViewStack.identity();
            RenderSystem.applyModelViewMatrix();

            int snapshotSize = Math.max(snapshot.size.getX(), Math.max(snapshot.size.getY(), snapshot.size.getZ()));
            snapshotPose.translate(0, 0, -snapshotSize * 2.0F - 3.0F);
            snapshotPose.rotateAround(Axis.XP.rotationDegrees(20), 1, 0, 0);
            snapshotPose.rotateAround(
                Axis.YP.rotationDegrees((System.currentTimeMillis() % 3600L) / 10.0F),
                0,
                1,
                0
            );
            snapshotPose.translate(
                -snapshot.size.getX() / 2.0F,
                -snapshot.size.getY() / 2.0F,
                -snapshot.size.getZ() / 2.0F
            );
            snapshotPose.translate(0, snapshotSize * 0.1F, 0);

            MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
            renderBlocks(minecraft, world, snapshot, snapshotPose, bufferSource);
            if (snapshotSize < 32) {
                renderBlockEntities(minecraft, world, snapshot, partialTick, snapshotPose, bufferSource);
            }
            renderEntities(minecraft, world, snapshotPose, bufferSource);
            bufferSource.endBatch();
        } finally {
            RenderSystem.viewport(0, 0, minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
            RenderSystem.restoreProjectionMatrix();
            modelViewStack.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        }
    }

    private static void renderBlocks(
        Minecraft minecraft,
        FakeWorld world,
        Snapshot snapshot,
        PoseStack pose,
        MultiBufferSource.BufferSource bufferSource
    ) {
        BlockRenderDispatcher blockRenderer = minecraft.getBlockRenderer();
        pose.pushPose();
        pose.translate(
            -FakeWorld.BLUEPRINT_OFFSET.getX(),
            -FakeWorld.BLUEPRINT_OFFSET.getY(),
            -FakeWorld.BLUEPRINT_OFFSET.getZ()
        );
        for (int z = 0; z < snapshot.size.getZ(); z++) {
            for (int y = 0; y < snapshot.size.getY(); y++) {
                for (int x = 0; x < snapshot.size.getX(); x++) {
                    BlockPos pos = new BlockPos(x, y, z).offset(FakeWorld.BLUEPRINT_OFFSET);
                    BlockState state = world.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }

                    pose.pushPose();
                    pose.translate(pos.getX(), pos.getY(), pos.getZ());
                    BakedModel model = blockRenderer.getBlockModel(state);
                    BlockEntity blockEntity = world.getBlockEntity(pos);
                    ModelData modelData = blockEntity == null ? ModelData.EMPTY : blockEntity.getModelData();
                    modelData = model.getModelData(world, pos, state, modelData);
                    for (RenderType renderType : model.getRenderTypes(state, world.random, modelData)) {
                        blockRenderer.renderSingleBlock(
                            state,
                            pose,
                            bufferSource,
                            COMBINED_LIGHT,
                            OverlayTexture.NO_OVERLAY,
                            modelData,
                            renderType
                        );
                    }
                    pose.popPose();
                }
            }
        }
        pose.popPose();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void renderBlockEntities(
        Minecraft minecraft,
        FakeWorld world,
        Snapshot snapshot,
        float partialTick,
        PoseStack pose,
        MultiBufferSource.BufferSource bufferSource
    ) {
        BlockEntityRenderDispatcher dispatcher = minecraft.getBlockEntityRenderDispatcher();
        for (int z = 0; z < snapshot.size.getZ(); z++) {
            for (int y = 0; y < snapshot.size.getY(); y++) {
                for (int x = 0; x < snapshot.size.getX(); x++) {
                    BlockPos pos = new BlockPos(x, y, z).offset(FakeWorld.BLUEPRINT_OFFSET);
                    BlockEntity blockEntity = world.getBlockEntity(pos);
                    if (blockEntity == null) {
                        continue;
                    }
                    BlockEntityRenderer renderer = dispatcher.getRenderer(blockEntity);
                    if (renderer == null) {
                        continue;
                    }

                    pose.pushPose();
                    pose.translate(x, y, z);
                    renderer.render(
                        blockEntity,
                        partialTick,
                        pose,
                        bufferSource,
                        COMBINED_LIGHT,
                        OverlayTexture.NO_OVERLAY
                    );
                    pose.popPose();
                }
            }
        }
    }

    private static void renderEntities(
        Minecraft minecraft,
        FakeWorld world,
        PoseStack pose,
        MultiBufferSource.BufferSource bufferSource
    ) {
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        for (Entity entity : world.getPreviewEntities()) {
            Vec3 pos = entity.getPosition(0);
            dispatcher.render(
                entity,
                pos.x - FakeWorld.BLUEPRINT_OFFSET.getX(),
                pos.y - FakeWorld.BLUEPRINT_OFFSET.getY(),
                pos.z - FakeWorld.BLUEPRINT_OFFSET.getZ(),
                0,
                0,
                pose,
                bufferSource,
                COMBINED_LIGHT
            );
        }
    }
}
