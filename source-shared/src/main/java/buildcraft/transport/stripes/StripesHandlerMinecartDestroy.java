/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.stripes;

import java.util.Collections;
import java.util.List;

import buildcraft.api.v2.OperationMode;
import buildcraft.api.v2.automation.StripesOutput;
import buildcraft.api.v2.permission.WorldOperationKind;
import buildcraft.lib.misc.AutomationPermissionUtil;
import buildcraft.lib.misc.StackUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public enum StripesHandlerMinecartDestroy {
    INSTANCE;

    public boolean handle(Level world, BlockPos pos, Direction direction, Player player, StripesOutput activator) {
        BlockPos target = pos.relative(direction);
        AABB box = new AABB(target);
        List<AbstractMinecart> minecarts = world.getEntitiesOfClass(AbstractMinecart.class, box);

        if (minecarts.size() > 0) {
            Collections.shuffle(minecarts);
            AbstractMinecart cart = minecarts.get(0);
            if (!AutomationPermissionUtil.mayEntity(
                world, pos, cart, player.getGameProfile(), AutomationPermissionUtil.SOURCE_STRIPES_PIPE,
                WorldOperationKind.ENTITY_ATTACK, OperationMode.EXECUTE
            )) {
                return false;
            }
            if (cart instanceof AbstractMinecartContainer) {
                // good job, Mojang. :<
            	AbstractMinecartContainer container = (AbstractMinecartContainer) cart;
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack s = container.getItem(i);
                    if (!s.isEmpty()) {
                        container.setChestVehicleItem(i, ItemStack.EMPTY);;
                        // Safety check
                        if (container.getItem(i).isEmpty()) {
                            activator.sendItem(s, direction);
                        }
                    }
                }
            }
            cart.kill();
            activator.sendItem(StackUtil.asNonNull(cart.getPickResult()), direction);
            return true;
        }
        return false;
    }
}
