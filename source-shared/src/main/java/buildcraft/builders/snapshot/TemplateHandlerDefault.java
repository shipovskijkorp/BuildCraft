/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.snapshot;

import buildcraft.api.v2.template.TemplateHandler;
import buildcraft.lib.misc.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public enum TemplateHandlerDefault implements TemplateHandler {
    INSTANCE;

    @Override
    public boolean handle(Level world, BlockPos pos, Player player, ItemStack stack) {
        return BlockUtil.useItemOnBlock(world, player, stack, pos, Direction.UP);
    }
}
