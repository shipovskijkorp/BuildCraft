/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.stripes;

import java.util.List;

import buildcraft.transport.internal.IStripesActivator;
import buildcraft.transport.internal.IStripesHandlerItem;
import buildcraft.lib.misc.BlockUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.IShearable;

public enum StripesHandlerShears implements IStripesHandlerItem {
    INSTANCE;

    @Override
    public boolean handle(Level world,
                          BlockPos pos,
                          Direction direction,
                          ItemStack stack,
                          Player player,
                          IStripesActivator activator) {
        if (!(stack.getItem() instanceof ShearsItem)) {
            return false;
        }

        pos = pos.offset(direction.getNormal());
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if (block instanceof IShearable) {
            if (!(world instanceof ServerLevel serverLevel) || !BlockUtil.canBreakBlock(serverLevel, pos, player)) {
                return false;
            }
        	IShearable shearableBlock = (IShearable) block;
            if (shearableBlock.isShearable(player, stack, world, pos)) {
                List<ItemStack> drops = shearableBlock.onSheared(player, stack, world, pos);
                stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
                for (ItemStack dropStack : drops) {
                    activator.sendItem(dropStack, direction);
                }
                return true;
            }
        }
        return false;
    }
}
