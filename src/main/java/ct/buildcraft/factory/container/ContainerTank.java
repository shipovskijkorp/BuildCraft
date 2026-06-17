/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package ct.buildcraft.factory.container;

import ct.buildcraft.factory.BCFactoryBlocks;
import ct.buildcraft.factory.BCFactoryGuis;
import ct.buildcraft.factory.tile.TileTank;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;

public class ContainerTank extends AbstractContainerMenu {
    private static final ForgeRegistry<Fluid> FLUIDS = (ForgeRegistry<Fluid>) ForgeRegistries.FLUIDS;

    private final ContainerLevelAccess access;
    public final ContainerData data;

    public ContainerTank(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainerData(2), ContainerLevelAccess.NULL);
    }

    public ContainerTank(int containerId, Inventory playerInventory, TileTank tank, ContainerLevelAccess access) {
        this(containerId, playerInventory, new TankData(tank), access);
    }

    private ContainerTank(int containerId, Inventory playerInventory, ContainerData data, ContainerLevelAccess access) {
        super(BCFactoryGuis.MENU_TANK.get(), containerId);
        this.access = access;
        this.data = data;

        addFullPlayerInventory(playerInventory, 8, 99);
        addDataSlots(data);
    }

    private void addFullPlayerInventory(Inventory playerInventory, int startX, int startY) {
        for (int sy = 0; sy < 3; sy++) {
            for (int sx = 0; sx < 9; sx++) {
                addSlot(new Slot(playerInventory, sx + sy * 9 + 9, startX + sx * 18, startY + sy * 18));
            }
        }

        for (int sx = 0; sx < 9; sx++) {
            addSlot(new Slot(playerInventory, sx, startX + sx * 18, startY + 58));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }

        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack before = slot.getItem().copy();
        ItemStack after = access.evaluate((level, pos) -> {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof TileTank tile)) {
                return slot.getItem();
            }

            ItemStack result = tile.tank.transferStackToTank(player, slot.getItem());
            tile.balanceTankFluids();
            tile.setChanged();
            return result;
        }).orElse(slot.getItem());

        if (!ItemStack.matches(after, before)) {
            slot.set(after);
            slot.setChanged();
            broadcastChanges();
            return before;
        }

        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, BCFactoryBlocks.TANK_BLOCK.get());
    }

    @Override
    public boolean clickMenuButton(Player player, int index) {
        return access.evaluate((level, pos) -> {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TileTank tile) {
                tile.tank.onGuiClicked(this, player);
                tile.balanceTankFluids();
                tile.setChanged();
                broadcastChanges();
                return true;
            }
            return false;
        }).orElse(false);
    }

    private static final class TankData implements ContainerData {
        private final TileTank tank;

        private TankData(TileTank tank) {
            this.tank = tank;
        }

        @Override
        public int get(int index) {
            FluidStack fluid = tank.tank.getFluid();
            return switch (index) {
                case 0 -> FLUIDS.getID(fluid.isEmpty() ? Fluids.EMPTY : fluid.getFluid());
                case 1 -> fluid.getAmount();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
