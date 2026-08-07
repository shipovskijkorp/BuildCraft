/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.container;

import buildcraft.lib.gui.ContainerBCTile;
import buildcraft.lib.gui.slot.SlotBase;
import buildcraft.lib.gui.slot.SlotDisplay;
import buildcraft.lib.gui.slot.SlotOutput;
import buildcraft.lib.gui.slot.SlotPhantom;
import buildcraft.lib.tile.item.IItemHandlerAdv;
import buildcraft.lib.tile.item.ItemHandlerSimple;
import buildcraft.silicon.BCSiliconGuis;
import buildcraft.silicon.tile.TileAdvancedCraftingTable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.neoforged.neoforge.items.IItemHandler;

public class ContainerAdvancedCraftingTable extends ContainerBCTile<TileAdvancedCraftingTable> {

    /** The exact handlers backing this menu's synced slots (client and server instances differ). */
    public final IItemHandlerAdv blueprintInv;
    public final IItemHandlerAdv materialInv;

    public ContainerAdvancedCraftingTable(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, new ItemHandlerSimple(15), new ItemHandlerSimple(9), new ItemHandlerSimple(9), new ItemHandlerSimple(1), createLevelAccess(playerInventory, buf));
    }

    public ContainerAdvancedCraftingTable(int containerId, Inventory playerInventory, IItemHandlerAdv invMaterials,
            IItemHandlerAdv invResults, IItemHandlerAdv invBlueprint, IItemHandler clientResult, ContainerLevelAccess access) {
        super(BCSiliconGuis.MENU_AD_CRAFTING_TABLE.get(), playerInventory, containerId, access);
        blueprintInv = invBlueprint;
        materialInv = invMaterials;

        addSlot(new SlotDisplay(clientResult, 0, 127, 33));

        for(int y = 0; y < 3; y++) {
            for(int x = 0; x < 3; x++) {
                addSlot(new SlotPhantom(invBlueprint, x + y * 3, 33 + x * 18, 16 + y * 18, false));
            }
        }

        for(int y = 0; y < 3; y++) {
            for(int x = 0; x < 5; x++) {
                addSlot(new SlotBase(invMaterials, x + y * 5, 15 + x * 18, 85 + y * 18));
            }
        }

        for(int y = 0; y < 3; y++) {
            for(int x = 0; x < 3; x++) {
                addSlot(new SlotOutput(invResults, x + y * 3, 109 + x * 18, 85 + y * 18));
            }
        }
        addFullPlayerInventory(153);
    }
}
