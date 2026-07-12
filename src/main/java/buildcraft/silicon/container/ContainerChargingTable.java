/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.container;

import buildcraft.lib.gui.ContainerBCTile;
import buildcraft.lib.gui.slot.SlotBase;
import buildcraft.lib.tile.item.IItemHandlerAdv;
import buildcraft.lib.tile.item.ItemHandlerSimple;
import buildcraft.silicon.BCSiliconGuis;
import buildcraft.silicon.tile.TileChargingTable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;

public class ContainerChargingTable extends ContainerBCTile<TileChargingTable> {

    public ContainerChargingTable(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, new ItemHandlerSimple(1), createLevelAccess(playerInventory, buf));
    }

    public ContainerChargingTable(int containerId, Inventory playerInventory, IItemHandlerAdv invCharge, ContainerLevelAccess access) {
        super(BCSiliconGuis.MENU_CHARGING_TABLE.get(), playerInventory, containerId, access);

        addSlot(new SlotBase(invCharge, 0, 80, 18));
        addFullPlayerInventory(50);
    }
}
