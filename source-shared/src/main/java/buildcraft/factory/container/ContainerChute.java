/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.container;

import buildcraft.factory.BCFactoryGuis;
import buildcraft.factory.tile.TileChute;
import buildcraft.lib.gui.ContainerBCTile;
import buildcraft.lib.gui.slot.SlotBase;
import buildcraft.lib.tile.item.IItemHandlerAdv;
import buildcraft.lib.tile.item.ItemHandlerSimple;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;

public class ContainerChute extends ContainerBCTile<TileChute> {

    public ContainerChute(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory, new ItemHandlerSimple(4), createLevelAccess(playerInventory, buffer));
    }

    public ContainerChute(int containerId, Inventory playerInventory, IItemHandlerAdv inventory,
        ContainerLevelAccess access) {
        super(BCFactoryGuis.MENU_CHUTE.get(), playerInventory, containerId, access);
        IItemHandlerAdv chuteInventory = tile != null ? tile.inv : inventory;

        addFullPlayerInventory(71);

        addSlot(new SlotBase(chuteInventory, 0, 62, 18));
        addSlot(new SlotBase(chuteInventory, 1, 80, 18));
        addSlot(new SlotBase(chuteInventory, 2, 98, 18));
        addSlot(new SlotBase(chuteInventory, 3, 80, 36));
    }
}
