/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */
package buildcraft.lib.fake;

import java.util.Collection;
import java.util.List;
import java.util.OptionalInt;

import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.stats.Stat;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.SignBlockEntity;

/**
 * BuildCraft automation player.
 *
 * <p>Modern Forge no longer exposes its old FakePlayer implementation, but vanilla
 * assumes every {@link ServerPlayer} has a non-null connection in many unrelated
 * paths (recipe rewards, menus, statistics and block callbacks). Keep a no-op
 * connection installed so a modded block cannot crash the server merely by trying
 * to send a packet to the automation actor.</p>
 */
public class FakePlayerBC extends ServerPlayer {
    public FakePlayerBC(ServerLevel level, GameProfile profile) {
        super(level.getServer(), level, profile, ClientInformation.createDefault());
        Connection connection = new DiscardingConnection();
        this.connection = new ServerGamePacketListenerImpl(
            level.getServer(), connection, this, CommonListenerCookie.createInitial(profile, false)
        );
    }

    @Override
    public void openTextEdit(SignBlockEntity sign, boolean frontText) {
        // Automation must never open a client sign editor.
    }

    @Override
    public OptionalInt openMenu(MenuProvider provider) {
        // A fake player has no client screen. More importantly, menu opening often
        // sends packets and may leave a synthetic container attached indefinitely.
        return OptionalInt.empty();
    }

    @Override
    public void awardStat(Stat<?> stat, int amount) {
        // Do not write player statistics for automation actors.
    }

    @Override
    public int awardRecipes(Collection<RecipeHolder<?>> recipes) {
        return 0;
    }

    @Override
    public void awardRecipesByKey(List<ResourceLocation> recipeIds) {
        // Recipe-book updates are client-only and must not target a fake player.
    }

    private static final class DiscardingConnection extends Connection {
        private DiscardingConnection() {
            super(PacketFlow.SERVERBOUND);
        }

        @Override
        public void send(Packet<?> packet) {
        }

        @Override
        public void send(Packet<?> packet, @Nullable PacketSendListener listener) {
        }

        @Override
        public void send(Packet<?> packet, @Nullable PacketSendListener listener, boolean flush) {
        }

        @Override
        public boolean isConnected() {
            return false;
        }
    }
}
