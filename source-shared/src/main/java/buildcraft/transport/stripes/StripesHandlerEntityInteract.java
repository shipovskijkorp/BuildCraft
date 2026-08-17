/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.stripes;

import java.util.Collections;
import java.util.List;

import buildcraft.api.v2.automation.StripesOutput;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public enum StripesHandlerEntityInteract {
    INSTANCE;

    public boolean handle(Level world,
                          BlockPos pos,
                          Direction direction,
                          ItemStack stack,
                          Player player,
                          StripesOutput activator) {
        List<LivingEntity> entities = world.getEntitiesOfClass(
            LivingEntity.class,
            new AABB(pos.offset(direction.getNormal()))
        );
        Collections.shuffle(entities);
        for (LivingEntity entity : entities) {
            if (player.interactOn(entity, InteractionHand.MAIN_HAND).consumesAction()) {
                return true;
            }
        }
        return false;
    }
}
