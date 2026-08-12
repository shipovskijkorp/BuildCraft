/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.misc;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

import buildcraft.api.core.BCLog;
import buildcraft.lib.fake.FakePlayerBC;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public enum FakePlayerProvider {
    INSTANCE;

    /** The default {@link GameProfile} to use if a tile entity cannot determine its real owner. Most of the time this
     * shouldn't be necessary, as we should be able to get a {@link GameProfile} from all {@link Player}'s that
     * place or create tiles/robots */
    public static final GameProfile NULL_PROFILE;

    static {
        UUID id = UUID.nameUUIDFromBytes("buildcraft.core".getBytes(StandardCharsets.UTF_8));
        NULL_PROFILE = new GameProfile(id, "[BuildCraft]");
    }

    /** Fake players are world-bound entities; never move one mutable instance between dimensions. */
    private final Map<ServerLevel, Map<GameProfile, FakePlayerBC>> playersByWorld = new IdentityHashMap<>();

    public FakePlayerBC getBuildCraftPlayer(ServerLevel world) {
        return getFakePlayer(world, NULL_PROFILE, BlockPos.ZERO);
    }

    public FakePlayerBC getFakePlayer(ServerLevel world, GameProfile profile) {
        return getFakePlayer(world, profile, BlockPos.ZERO);
    }

    public FakePlayerBC getFakePlayer(ServerLevel world, GameProfile profile, BlockPos pos) {
        if (profile == null) {
            BCLog.logger.warn("[lib.fake] Null GameProfile! This is a bug!", new IllegalArgumentException());
            profile = NULL_PROFILE;
        }
        Map<GameProfile, FakePlayerBC> worldPlayers =
            playersByWorld.computeIfAbsent(world, ignored -> new HashMap<>());
        FakePlayerBC player = worldPlayers.computeIfAbsent(profile, p -> new FakePlayerBC(world, p));
        player.setPos(pos.getX(), pos.getY(), pos.getZ());
        return player;
    }

    public void unloadWorld(ServerLevel world) {
        playersByWorld.remove(world);
    }
}
