/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.menu;

import java.io.IOException;

import buildcraft.builders.BCBuildersBlocks;
import buildcraft.builders.BCBuildersGuis;
import buildcraft.builders.tile.TileArchitectTable;
import buildcraft.lib.gui.ContainerBCTile;
import buildcraft.lib.gui.MenuBC_Neptune;
import buildcraft.lib.gui.slot.SlotBase;
import buildcraft.lib.gui.slot.SlotOutput;
import buildcraft.lib.misc.data.IdAllocator;
import buildcraft.lib.tile.TileBC_Neptune;
import buildcraft.lib.tile.item.IItemHandlerAdv;
import buildcraft.lib.tile.item.ItemHandlerSimple;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ContainerArchitectTable extends ContainerBCTile<TileArchitectTable> {
    private static final IdAllocator IDS = MenuBC_Neptune.IDS.makeChild("architect_table");
    private static final int ID_NAME = IDS.allocId("NAME");
    private static final int ID_SETTINGS = IDS.allocId("SETTINGS");
    public String name = "";
    public final DataSlot setting;
    public final DataSlot creativePermission;
   // public final ContainerData deltaProgress;
    
	public ContainerArchitectTable(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
		this(containerId, playerInventory, new ItemHandlerSimple(1), new ItemHandlerSimple(1), DataSlot.standalone(), DataSlot.standalone(), createLevelAccess(playerInventory, buf));
	}

    public ContainerArchitectTable(int containerId, Inventory playerInventory, IItemHandlerAdv in, IItemHandlerAdv out,
        DataSlot setting, DataSlot creativePermission, ContainerLevelAccess access) {
		super(BCBuildersGuis.MENU_ARCHITECT_TABLE.get(), playerInventory, containerId, access);
        addFullPlayerInventory(88, 84);

        addSlot(new SlotBase(in, 0, 135, 35));
        addSlot(new SlotOutput(out, 0, 194, 35));
        
        this.setting = setting;
        this.creativePermission = creativePermission;
        addDataSlot(setting);
        addDataSlot(creativePermission);
        
//        this.deltaProgress = containerData;
       // addDataSlots(containerData);
        
    }
    
    @Override
    public IdAllocator getIdAllocator() {
        return IDS;
    }

    public void sendNameToServer(String name) {
    	if(!this.name.equals(name)) {
    		sendMessage(ID_NAME, buffer -> buffer.writeUtf(name));
    		this.name = name;
    	}
    }

    public void sendSettingsToServer(int settings) {
        sendMessage(ID_SETTINGS, buffer -> buffer.writeVarInt(settings));
        this.setting.set(settings);
    }

    @Override
    public void readMessage(int id, FriendlyByteBuf buffer, LogicalSide side, IPayloadContext ctx) throws IOException {
        super.readMessage(id, buffer, side, ctx);
        if (side == LogicalSide.SERVER) {
            if (id == ID_NAME) {
                tile.name = buffer.readUtf();
                tile.sendNetworkUpdate(TileBC_Neptune.NET_RENDER_DATA);
            } else if (id == ID_SETTINGS) {
                Player sender = ctx == null ? playerInventory.player : ctx.player();
                tile.setSnapshotSettingsFromPlayer(buffer.readVarInt(), sender);
            }
        }
 /*       if (side == LogicalSide.CLIENT) {
            if (id == ID_NAME) {
                name = buffer.readUtf();
            }
        }*/
    }

	@Override
	public boolean stillValid(Player player) {
		return super.stillValid(access, player, BCBuildersBlocks.ARCHITECT.get());
	}

/*	@Override
	public void clientInit(FriendlyByteBuf data) {
		name = data.readUtf();
	}*/
}
