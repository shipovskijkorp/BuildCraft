/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.factory.tile;

import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.container.ContainerAutoCraftItems;
import buildcraft.lib.gui.ItemProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class TileAutoWorkbenchItems extends TileAutoWorkbenchBase implements MenuProvider, Container{
    public TileAutoWorkbenchItems(BlockPos pos, BlockState state) {
        super(BCFactoryBlocks.ENTITYBLOCKAUTOBENCH.get(), pos, state, 3, 3);
    }

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new ContainerAutoCraftItems(id, inventory, invResult, invBlueprint, invMaterialFilter, 
				invMaterials, new ItemProvider(i -> resultClient, 1), ContainerLevelAccess.create(level, worldPosition));
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable(this.getBlockState().getBlock().getDescriptionId());
	}

	@Override
	public void clearContent() {
	}

	@Override
	public int getContainerSize() {
		// This Container view exists for vanilla/robotics extraction compatibility. Phantom
		// blueprint/material-filter slots are internal configuration and must never be exposed
		// as real inventory contents. The sided item capability likewise exposes only result
		// extraction and material insertion.
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return invResult.getStackInSlot(0).isEmpty();
	}

	@Override
	public ItemStack getItem(int index) {
		return index == 0 ? invResult.getStackInSlot(0) : ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItem(int index, int num) {
		return index == 0 ? invResult.extractItem(0, num, false) : ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItemNoUpdate(int index) {
		return index == 0 ? invResult.extractItem(0, 64, false) : ItemStack.EMPTY;
	}

	@Override
	public void setItem(int index, ItemStack item) {
		// Output-only Container facade: insertion is intentionally rejected.
	}

	@Override
	public boolean canPlaceItem(int index, ItemStack item) {
		return false;
	}

	@Override
	public boolean stillValid(Player plyer) {
		return !this.remove;
	}
}
