/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */
package buildcraft.core.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Unbreakable;

public class ItemGoggles extends ArmorItem {
    public ItemGoggles(String id) {
        super(ArmorMaterials.CHAIN, ArmorItem.Type.HELMET, new Item.Properties()
            .stacksTo(1)
            .component(DataComponents.UNBREAKABLE, new Unbreakable(false)));
    }

    @Override
    public float getToughness() {
        return 0;
    }

}
