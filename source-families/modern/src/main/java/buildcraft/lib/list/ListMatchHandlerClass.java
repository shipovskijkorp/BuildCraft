/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.list;

import javax.annotation.Nonnull;

import buildcraft.api.lists.ListMatchHandler;
import buildcraft.api.lists.ListRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

public class ListMatchHandlerClass extends ListMatchHandler {
    @Override
    public boolean matches(Type type, @Nonnull ItemStack stack, @Nonnull ItemStack target, boolean precise) {
        if (type != Type.TYPE) {
            return false;
        }

        // ItemFood no longer exists in modern Minecraft. Item#isEdible is the
        // closest equivalent to the old "all food items are one type" rule.
        if (stack.has(DataComponents.FOOD)) {
            return target.has(DataComponents.FOOD);
        }

        Class<?> itemClass = stack.getItem().getClass();
        return ListRegistry.itemClassAsType.contains(itemClass)
            && itemClass.equals(target.getItem().getClass());
    }

    @Override
    public boolean isValidSource(Type type, @Nonnull ItemStack stack) {
        if (type != Type.TYPE) {
            return false;
        }
        return stack.has(DataComponents.FOOD) || ListRegistry.itemClassAsType.contains(stack.getItem().getClass());
    }
}
