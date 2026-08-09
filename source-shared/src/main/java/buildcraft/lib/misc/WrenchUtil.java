/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 */
package buildcraft.lib.misc;

import buildcraft.api.tools.IToolWrench;
import buildcraft.lib.BCLibConfig;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;

/** Bridges the legacy BuildCraft wrench interface with the common wrench item tag. */
public final class WrenchUtil {
    private static final String WRENCH_TAG_NAMESPACE = "c";
    private static final String WRENCH_TAG_PATH = "tools/wrench";

    private WrenchUtil() {
    }

    /** Returns true for legacy BuildCraft-compatible wrenches and, when enabled, common tagged wrenches. */
    public static boolean isWrench(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof IToolWrench) {
            return true;
        }
        return BCLibConfig.useWrenchTag && stack.getTags().anyMatch(tag ->
            WRENCH_TAG_NAMESPACE.equals(tag.location().getNamespace())
                && WRENCH_TAG_PATH.equals(tag.location().getPath()));
    }

    /** Notifies legacy interface-based wrenches after use; tag-only tools have no BuildCraft callback to invoke. */
    public static void wrenchUsed(Player player, InteractionHand hand, ItemStack stack, HitResult hit) {
        if (stack.getItem() instanceof IToolWrench wrench) {
            wrench.wrenchUsed(player, hand, stack, hit);
        }
    }
}
