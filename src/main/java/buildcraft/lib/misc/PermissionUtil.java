/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.misc;

import java.util.Objects;

import javax.annotation.Nullable;

import buildcraft.api.core.IPlayerOwned;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;

/** Shared permission checks for BuildCraft-owned blocks and machines. */
public class PermissionUtil {
    // Object types are retained for API compatibility with BC8 callers.
    public static final Object PERM_VIEW = "buildcraft.view";
    public static final Object PERM_EDIT = "buildcraft.edit";
    public static final Object PERM_DESTROY = "buildcraft.destroy";

    private static final int MAX_INTERACT_DISTANCE = 8;
    private static final int MAX_INTERACT_DISTANCE_SQ = MAX_INTERACT_DISTANCE * MAX_INTERACT_DISTANCE;

    private PermissionUtil() {
    }

    /** Checks whether one owned BuildCraft block may modify another block. */
    public static boolean hasPermission(Object type, PermissionBlock attempting, PermissionBlock target) {
        if (attempting == null || target == null) {
            return false;
        }
        if (type == PERM_VIEW) {
            return true;
        }
        return canModify(ownerOf(attempting), ownerOf(target));
    }

    /** Checks a profile against the target block's owner. */
    public static boolean hasPermission(Object type, GameProfile attempting, PermissionBlock target) {
        if (attempting == null || target == null) {
            return false;
        }
        if (type == PERM_VIEW) {
            return true;
        }
        return canModify(attempting, ownerOf(target));
    }

    /** Checks player range, vanilla spawn protection, ownership, and destructive Forge protection hooks. */
    public static boolean hasPermission(Object type, Player attempting, PermissionBlock target) {
        if (attempting == null || target == null) {
            return false;
        }
        if (attempting.blockPosition().distSqr(target.pos) > MAX_INTERACT_DISTANCE_SQ) {
            return false;
        }

        Level level = target.level != null ? target.level : attempting.level;
        if (type != PERM_VIEW && !level.isClientSide && !level.mayInteract(attempting, target.pos)) {
            return false;
        }

        // Server operators retain the normal administrative bypass.
        if (type != PERM_VIEW && !attempting.hasPermissions(2)
            && !canModify(attempting.getGameProfile(), ownerOf(target))) {
            return false;
        }

        // Only destructive probes post BreakEvent. Posting interaction events from menu validity checks would
        // repeatedly fire them every tick while a GUI is open. Normal right-click claim checks have already run
        // before block use; actual BuildCraft block breaking also posts BreakEvent at the operation site.
        if (type == PERM_DESTROY && !level.isClientSide) {
            BreakEvent event = new BreakEvent(level, target.pos, level.getBlockState(target.pos), attempting);
            MinecraftForge.EVENT_BUS.post(event);
            if (event.isCanceled()) {
                return false;
            }
        }
        return true;
    }

    public static PermissionBlock createFrom(Level world, BlockPos pos) {
        BlockEntity tile = world.getBlockEntity(pos);
        IPlayerOwned owned = tile instanceof IPlayerOwned playerOwned ? playerOwned : null;
        return new PermissionBlock(owned, pos, world);
    }

    private static boolean canModify(@Nullable GameProfile attempting, @Nullable GameProfile targetOwner) {
        // Unowned/legacy blocks remain usable. Unknown BuildCraft fallback ownership is also treated as unowned.
        if (!isRealOwner(targetOwner)) {
            return true;
        }
        return isRealOwner(attempting) && profilesEqual(attempting, targetOwner);
    }

    private static boolean isRealOwner(@Nullable GameProfile profile) {
        return profile != null
            && !Objects.equals(profile.getId(), FakePlayerProvider.NULL_PROFILE.getId())
            && (profile.getId() != null || profile.getName() != null && !profile.getName().isEmpty());
    }

    private static boolean profilesEqual(GameProfile first, GameProfile second) {
        if (first.getId() != null && second.getId() != null) {
            return first.getId().equals(second.getId());
        }
        return first.getName() != null && second.getName() != null
            && first.getName().equalsIgnoreCase(second.getName());
    }

    @Nullable
    private static GameProfile ownerOf(PermissionBlock block) {
        return block.owned == null ? null : block.owned.getOwner();
    }

    public static class PermissionBlock {
        @Nullable
        public final IPlayerOwned owned;
        public final BlockPos pos;
        @Nullable
        public final Level level;

        public PermissionBlock(@Nullable IPlayerOwned owned, BlockPos pos) {
            this(owned, pos, null);
        }

        public PermissionBlock(@Nullable IPlayerOwned owned, BlockPos pos, @Nullable Level level) {
            this.owned = owned;
            this.pos = pos;
            this.level = level;
        }
    }
}
