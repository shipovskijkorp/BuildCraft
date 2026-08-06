/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.list;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import buildcraft.api.lists.ListMatchHandler;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

public class ListMatchHandlerTools extends ListMatchHandler {
    private static final ToolAction[] TOOL_TYPES = {
        ToolActions.AXE_DIG,
        ToolActions.PICKAXE_DIG,
        ToolActions.SHOVEL_DIG,
        ToolActions.HOE_DIG,
        ToolActions.SWORD_DIG,
        ToolActions.SHEARS_DIG
    };

    private static Set<ToolAction> getToolTypes(ItemStack stack) {
        Set<ToolAction> actions = new HashSet<>();
        for (ToolAction action : TOOL_TYPES) {
            if (stack.canPerformAction(action)) {
                actions.add(action);
            }
        }
        return actions;
    }

    @Override
    public boolean matches(Type type, @Nonnull ItemStack stack, @Nonnull ItemStack target, boolean precise) {
        if (type != Type.TYPE) {
            return false;
        }

        Set<ToolAction> sourceTypes = getToolTypes(stack);
        Set<ToolAction> targetTypes = getToolTypes(target);
        if (sourceTypes.isEmpty() || targetTypes.isEmpty()) {
            return false;
        }

        return precise ? sourceTypes.equals(targetTypes) : targetTypes.containsAll(sourceTypes);
    }

    @Override
    public boolean isValidSource(Type type, @Nonnull ItemStack stack) {
        return type == Type.TYPE && !getToolTypes(stack).isEmpty();
    }
}
