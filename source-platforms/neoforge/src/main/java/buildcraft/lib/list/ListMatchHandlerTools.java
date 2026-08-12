/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.list;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import buildcraft.api.v2.list.ListMatchType;
import buildcraft.lib.list.ListMatchHandlerBackend;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ItemAbility;

public class ListMatchHandlerTools extends ListMatchHandlerBackend {
    private static final ItemAbility[] TOOL_TYPES = {
        ItemAbility.get("axe_dig"),
        ItemAbility.get("pickaxe_dig"),
        ItemAbility.get("shovel_dig"),
        ItemAbility.get("hoe_dig"),
        ItemAbility.get("sword_dig"),
        ItemAbility.get("shears_dig")
    };

    private static Set<ItemAbility> getToolTypes(ItemStack stack) {
        Set<ItemAbility> actions = new HashSet<>();
        for (ItemAbility action : TOOL_TYPES) {
            if (stack.canPerformAction(action)) {
                actions.add(action);
            }
        }
        return actions;
    }

    @Override
    public boolean matches(ListMatchType type, @Nonnull ItemStack stack, @Nonnull ItemStack target, boolean precise) {
        if (type != ListMatchType.TYPE) {
            return false;
        }

        Set<ItemAbility> sourceTypes = getToolTypes(stack);
        Set<ItemAbility> targetTypes = getToolTypes(target);
        if (sourceTypes.isEmpty() || targetTypes.isEmpty()) {
            return false;
        }

        return precise ? sourceTypes.equals(targetTypes) : targetTypes.containsAll(sourceTypes);
    }

    @Override
    public boolean isValidSource(ListMatchType type, @Nonnull ItemStack stack) {
        return type == ListMatchType.TYPE && !getToolTypes(stack).isEmpty();
    }
}
