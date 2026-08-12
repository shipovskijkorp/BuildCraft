/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.stripes;

import buildcraft.api.v2.BuildCraftApi;
import buildcraft.api.v2.BuildCraftServices;
import buildcraft.api.v2.automation.StripesOutput;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public enum StripesHandlerPlant {
    INSTANCE;

    public boolean handle(Level world,
                          BlockPos pos,
                          Direction direction,
                          ItemStack stack,
                          Player player,
                          StripesOutput activator) {
        return BuildCraftApi.service(BuildCraftServices.CROPS).plant(world, player, stack, pos.offset(direction.getNormal()).below())
            || BuildCraftApi.service(BuildCraftServices.CROPS).plant(world, player, stack, pos.offset(direction.getNormal()));
    }
}
