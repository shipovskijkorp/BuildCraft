/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.chunkload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import buildcraft.api.core.BCLog;
import buildcraft.lib.BCLib;
import buildcraft.lib.BCLibConfig;
import buildcraft.lib.chunkload.IChunkLoadingTile.LoadType;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.event.server.ServerStoppingEvent;

/**
 * Forge-backed replacement for BuildCraft's removed chunk-ticket manager.
 *
 * <p>Tickets are persisted by Forge and owned by the loading block's position. The local mirror is used to release
 * chunks when a quarry changes size and to validate persisted owners after their own chunk has loaded.</p>
 */
public final class ChunkLoaderManager {
    private static final Map<ServerLevel, Map<BlockPos, Set<ChunkPos>>> LOADED_CHUNKS = new WeakHashMap<>();
    private static boolean initialized;

    private ChunkLoaderManager() {
    }

    /** Registers the Forge ticket callback and the delayed persisted-ticket validator. */
    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        ForgeChunkManager.setForcedChunkLoadingCallback(BCLib.MODID, ChunkLoaderManager::validateTickets);
        MinecraftForge.EVENT_BUS.addListener(ChunkLoaderManager::onServerStopping);
    }

    /** Applies the chunks currently requested by a loading tile and releases obsolete tickets. */
    public static <T extends BlockEntity & IChunkLoadingTile> void loadChunksForTile(T tile) {
        if (!(tile.getLevel() instanceof ServerLevel level) || tile.isRemoved()) {
            return;
        }
        loadChunksForTile(level, tile, tile);
    }

    private static void loadChunksForTile(ServerLevel level, BlockEntity owner, IChunkLoadingTile loadingTile) {
        BlockPos ownerPos = owner.getBlockPos().immutable();
        if (!canLoadFor(level, loadingTile)) {
            releaseChunksFor(level, ownerPos, getTrackedChunks(level, ownerPos));
            return;
        }

        Set<ChunkPos> wanted = getChunksToLoad(owner, loadingTile);
        Map<BlockPos, Set<ChunkPos>> levelChunks = LOADED_CHUNKS.computeIfAbsent(level, ignored -> new HashMap<>());
        Set<ChunkPos> previous = levelChunks.getOrDefault(ownerPos, Collections.emptySet());

        Set<ChunkPos> obsolete = new HashSet<>(previous);
        obsolete.removeAll(wanted);
        for (ChunkPos chunk : obsolete) {
            unforceChunk(level, ownerPos, chunk);
        }

        for (ChunkPos chunk : wanted) {
            // Remove a legacy non-ticking form before adding the fully ticking machine ticket.
            ForgeChunkManager.forceChunk(level, BCLib.MODID, ownerPos, chunk.x, chunk.z, false, false);
            ForgeChunkManager.forceChunk(level, BCLib.MODID, ownerPos, chunk.x, chunk.z, true, true);
        }

        levelChunks.put(ownerPos, new HashSet<>(wanted));
    }

    /** Releases every ticket currently associated with this tile. */
    public static <T extends BlockEntity & IChunkLoadingTile> void releaseChunksFor(T tile) {
        if (!(tile.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos ownerPos = tile.getBlockPos().immutable();
        Set<ChunkPos> chunks = new HashSet<>(getTrackedChunks(level, ownerPos));
        // Also covers a newly placed tile that has not yet been mirrored in LOADED_CHUNKS.
        chunks.addAll(getChunksToLoad(tile, tile));
        releaseChunksFor(level, ownerPos, chunks);
    }

    private static Set<ChunkPos> getTrackedChunks(ServerLevel level, BlockPos ownerPos) {
        Map<BlockPos, Set<ChunkPos>> levelChunks = LOADED_CHUNKS.get(level);
        if (levelChunks == null) {
            return Collections.emptySet();
        }
        return levelChunks.getOrDefault(ownerPos, Collections.emptySet());
    }

    private static void releaseChunksFor(ServerLevel level, BlockPos ownerPos, Set<ChunkPos> chunks) {
        for (ChunkPos chunk : new HashSet<>(chunks)) {
            unforceChunk(level, ownerPos, chunk);
        }
        Map<BlockPos, Set<ChunkPos>> levelChunks = LOADED_CHUNKS.get(level);
        if (levelChunks != null) {
            levelChunks.remove(ownerPos);
            if (levelChunks.isEmpty()) {
                LOADED_CHUNKS.remove(level);
            }
        }
    }

    private static void unforceChunk(ServerLevel level, BlockPos ownerPos, ChunkPos chunk) {
        // Release both forms so old saves cannot retain a non-ticking ticket.
        ForgeChunkManager.forceChunk(level, BCLib.MODID, ownerPos, chunk.x, chunk.z, false, true);
        ForgeChunkManager.forceChunk(level, BCLib.MODID, ownerPos, chunk.x, chunk.z, false, false);
    }

    public static <T extends BlockEntity & IChunkLoadingTile> Set<ChunkPos> getChunksToLoad(T tile) {
        return getChunksToLoad(tile, tile);
    }

    private static Set<ChunkPos> getChunksToLoad(BlockEntity owner, IChunkLoadingTile loadingTile) {
        Set<ChunkPos> requested = loadingTile.getChunksToLoad();
        Set<ChunkPos> chunks = new HashSet<>(requested == null ? Collections.emptySet() : requested);
        chunks.add(new ChunkPos(owner.getBlockPos()));
        return chunks;
    }

    private static boolean canLoadFor(ServerLevel level, IChunkLoadingTile tile) {
        LoadType loadType = tile.getLoadType();
        return loadType != null && isEnabledFor(level) && BCLibConfig.chunkLoadingLevel.canLoad(loadType);
    }

    private static boolean isEnabledFor(ServerLevel level) {
        return switch (BCLibConfig.chunkLoadingType) {
            case ON -> true;
            case AUTO -> !level.getServer().isDedicatedServer();
            case OFF -> false;
        };
    }

    /**
     * Drops tickets persisted by the previous unsafe implementation.
     *
     * <p>Forge reinstates accepted tickets while the world is still loading. Persisting a quarry's entire work area can
     * therefore stall the integrated server before the player joins. Runtime tickets are recreated by the quarry after
     * its owner chunk has finished loading.</p>
     */
    private static void validateTickets(ServerLevel level, ForgeChunkManager.TicketHelper helper) {
        int removedOwners = 0;
        for (BlockPos owner : new HashSet<>(helper.getBlockTickets().keySet())) {
            helper.removeAllTickets(owner);
            removedOwners++;
        }
        LOADED_CHUNKS.remove(level);
        if (removedOwners > 0) {
            BCLog.logger.warn("[lib.chunkloading] Removed persisted tickets for {} owner(s) during safe migration", removedOwners);
        }
    }

    /**
     * Runtime quarry tickets are deliberately released before the final world save. They are restored when the quarry
     * chunk is loaded in a later session, rather than being reinstated inside Forge's world-startup chunk pipeline.
     */
    private static void onServerStopping(ServerStoppingEvent event) {
        List<ServerLevel> levels = new ArrayList<>();
        for (ServerLevel level : LOADED_CHUNKS.keySet()) {
            if (level.getServer() == event.getServer()) {
                levels.add(level);
            }
        }
        for (ServerLevel level : levels) {
            Map<BlockPos, Set<ChunkPos>> owners = LOADED_CHUNKS.get(level);
            if (owners == null) {
                continue;
            }
            for (Map.Entry<BlockPos, Set<ChunkPos>> entry : new HashMap<>(owners).entrySet()) {
                releaseChunksFor(level, entry.getKey(), new HashSet<>(entry.getValue()));
            }
        }
    }

}
