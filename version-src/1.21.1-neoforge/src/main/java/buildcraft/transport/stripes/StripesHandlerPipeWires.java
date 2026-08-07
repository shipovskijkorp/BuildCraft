/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.stripes;

import buildcraft.api.transport.IStripesActivator;
import buildcraft.api.transport.IStripesHandlerItem;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.lib.misc.ItemStackUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class StripesHandlerPipeWires implements IStripesHandlerItem {

    private static final int PIPES_TO_TRY = 8;

    @Override
    public boolean handle(Level world, BlockPos pos, Direction direction, ItemStack stack, Player player, IStripesActivator activator) {
        CompoundTag tag = ItemStackUtil.getCustomData(stack);
        DyeColor pipeWireColor = DyeColor.byId(tag.contains("color") ? tag.getInt("color") : 0);

        for (int i = PIPES_TO_TRY; i > 0; i--) {
            pos = pos.offset(direction.getOpposite().getNormal());

            IPipeHolder pipeHolder = world.getCapability(PipeApi.CAP_PIPE_HOLDER, pos, null);
            if (pipeHolder != null) {

                /*
                if (!pipeHolder.pipe.wireSet[pipeWireColor]) {
                    pipeHolder.pipe.wireSet[pipeWireColor] = true;
                    pipeHolder.pipe.signalStrength[pipeWireColor] = 0;

                    pipeHolder.pipe.updateSignalState();
                    pipeHolder.scheduleRenderUpdate();
                    world.notifyNeighborsOfStateChange(pipeHolder.getPipePos(), tile.getBlockType(), false);
                    */
                //stack.shrink(1);
                    /*
                    return true;

            }
            */

            } else {
                // Not a pipe, don't follow chain
                return false;
            }
        }

        return false;
    }
}
