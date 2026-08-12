/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.tile;

import buildcraft.lib.internal.core.EnumPipePart;
import buildcraft.lib.tile.TileBC_Neptune;
import buildcraft.lib.tile.item.ItemHandlerFiltered;
import buildcraft.lib.tile.item.ItemHandlerManager.EnumAccess;
import buildcraft.lib.tile.item.ItemHandlerSimple;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.container.ContainerFilteredBuffer_BC8;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.state.BlockState;

public class TileFilteredBuffer extends TileBC_Neptune implements MenuProvider{
    public final ItemHandlerSimple invFilter;
    public final ItemHandlerFiltered invMain;

    public TileFilteredBuffer(BlockPos pos, BlockState state) {
    	super(BCTransportBlocks.FILTERREDBUFFER_BE.get(), pos, state);
        invFilter = itemManager.addInvHandler("filter", 9, EnumAccess.PHANTOM);
        invFilter.setLimitedInsertor(1);

        invMain = new ItemHandlerFiltered(invFilter, false);
        itemManager.addInvHandler("main", invMain, EnumAccess.BOTH, EnumPipePart.VALUES);
    }
    
	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new ContainerFilteredBuffer_BC8(id, inventory, invFilter, invMain, ContainerLevelAccess.create(level, worldPosition));
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable(this.getBlockState().getBlock().getDescriptionId());
	}
}
