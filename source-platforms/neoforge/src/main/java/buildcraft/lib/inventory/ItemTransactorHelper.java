/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.inventory;

import javax.annotation.Nonnull;

import buildcraft.lib.internal.core.IStackFilter;
import buildcraft.lib.internal.inventory.IItemTransactor;
import buildcraft.lib.internal.inventory.IItemTransactor.IItemInsertable;
import buildcraft.transport.internal.IInjectable;
import buildcraft.transport.internal.pipe.PipeApi;
import buildcraft.lib.misc.BlockUtil;
import buildcraft.lib.misc.CapUtil;
import buildcraft.lib.misc.InventoryUtil;
import buildcraft.lib.misc.StackUtil;

import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

public class ItemTransactorHelper {
    @Nonnull
    public static IItemTransactor getTransactor(BlockEntity provider, Direction face) {
        if (provider == null || provider.getLevel() == null) {
            return NoSpaceTransactor.INSTANCE;
        }

        Level level = provider.getLevel();
        IItemTransactor trans = level.getCapability(
            CapUtil.CAP_ITEM_TRANSACTOR,
            provider.getBlockPos(),
            face
        );
        if (trans != null) {
            return trans;
        }

        Container doubleChest = BlockUtil.getCombinedDoubleChestContainer(provider);
        if (doubleChest != null) {
            return new InventoryWrapper(doubleChest);
        }

        IItemHandler handler = level.getCapability(CapUtil.CAP_ITEMS, provider.getBlockPos(), face);
        if (handler == null) {
            if (provider instanceof ISidedInventory sidedInventory) {
                return new SidedInventoryWrapper(sidedInventory, face);
            }
            if (provider instanceof Container container) {
                return new InventoryWrapper(container);
            }
            return NoSpaceTransactor.INSTANCE;
        }
        if (handler instanceof IItemTransactor itemTransactor) {
            return itemTransactor;
        }
        return new ItemHandlerWrapper(handler);
    }

    @Nonnull
    public static IItemTransactor getTransactor(Inventory inventory) {
        if (inventory == null) {
            return NoSpaceTransactor.INSTANCE;
        }
        return new InventoryWrapper(inventory);
    }

    @Nonnull
    public static IItemTransactor getTransactorForEntity(Entity entity, Direction face) {
        if (entity == null) {
            return NoSpaceTransactor.INSTANCE;
        }

        IItemTransactor transactor = entity.getCapability(CapUtil.CAP_ITEM_TRANSACTOR_ENTITY, face);
        if (transactor != null) {
            return transactor;
        }

        IItemHandler handler = entity.getCapability(Capabilities.ItemHandler.ENTITY_AUTOMATION, face);
        if (handler == null) {
            handler = entity.getCapability(Capabilities.ItemHandler.ENTITY);
        }
        if (handler instanceof IItemTransactor itemTransactor) {
            return itemTransactor;
        }
        if (handler != null) {
            return new ItemHandlerWrapper(handler);
        }
        if (entity instanceof ItemEntity itemEntity) {
            return new TransactorEntityItem(itemEntity);
        }
        if (entity instanceof AbstractArrow arrow) {
            return new TransactorEntityArrow(arrow);
        }
        return NoSpaceTransactor.INSTANCE;
    }

    @Nonnull
    public static IInjectable getInjectable(BlockEntity provider, Direction face) {
        if (provider == null || provider.getLevel() == null) {
            return NoSpaceInjectable.INSTANCE;
        }
        IInjectable injectable = provider.getLevel().getCapability(
            PipeApi.CAP_INJECTABLE,
            provider.getBlockPos(),
            face
        );
        return injectable == null ? NoSpaceInjectable.INSTANCE : injectable;
    }

    public static IItemTransactor wrapInjectable(IInjectable injectable, Direction facing) {
        return new InjectableWrapper(injectable, facing);
    }

    /** Provides an implementation of {@link IItemTransactor#insert(NonNullList, boolean)} that relies on
     * {@link IItemTransactor#insert(ItemStack, boolean, boolean)}. This is the least efficient, default
     * implementation. */
    public static NonNullList<ItemStack> insertAllBypass(IItemTransactor transactor, NonNullList<ItemStack> stacks, boolean simulate) {
        NonNullList<ItemStack> leftOver = NonNullList.create();
        for (ItemStack stack : stacks) {
            ItemStack leftOverStack = transactor.insert(stack, false, simulate);
            if (!leftOverStack.isEmpty()) {
                leftOver.add(leftOverStack);
            }
        }
        return leftOver;
    }

    /** Attempts to move as many items as possible from the source {@link IItemTransactor} to the destination.
     * 
     * @return The number of items moved. */
    public static int move(IItemTransactor src, IItemTransactor dst) {
        return move(src, dst, Integer.MAX_VALUE);
    }

    /** Attempts to move up to maxItems from the source {@link IItemTransactor} to the destination.
     * 
     * @param maxItems The maximum number of items to move.
     * @return The number of items moved. */
    public static int move(IItemTransactor src, IItemTransactor dst, int maxItems) {
        return move(src, dst, null, maxItems);
    }

    /** Attempts to move up to maxItems from the source {@link IItemTransactor} to the destination.
     * 
     * @return The number of items moved. */
    public static int move(IItemTransactor src, IItemTransactor dst, IStackFilter filter) {
        return move(src, dst, filter, Integer.MAX_VALUE);
    }

    /** Attempts to move up to maxItems from the source {@link IItemTransactor} to the destination.
     * 
     * @param filter The stack filter to use - only items that match this filter will be moved.
     * @param maxItems The maximum number of items to move.
     * @return The number of items moved. */
    public static int move(IItemTransactor src, IItemTransactor dst, IStackFilter filter, int maxItems) {
        int moved = 0;
        IStackFilter rFilter = dst::canPartiallyAccept;
        if (filter != null) {
            rFilter = rFilter.and(filter);
        }
        while (true) {
            int m = moveSingle0(src, dst, rFilter, maxItems - moved, false, false);
            if (m == 0) {
                break;
            } else {
                moved += m;
            }
        }
        return moved;
    }

    public static int moveSingle(IItemTransactor src, IItemTransactor dst, IStackFilter filter, boolean simulateSrc, boolean simulateDst) {
        return moveSingle(src, dst, filter, Integer.MAX_VALUE, simulateSrc, simulateDst);
    }

    /** Similar to {@link #move(IItemTransactor, IItemTransactor, IStackFilter, int)}, but will only attempt to extract
     * and insert once, which means that you can simulate the move safely. */
    public static int moveSingle(IItemTransactor src, IItemTransactor dst, IStackFilter filter, int maxItems, boolean simulateSrc, boolean simulateDst) {
        IStackFilter rFilter = dst::canPartiallyAccept;
        if (filter != null) {
            rFilter = rFilter.and(filter);
        }
        return moveSingle0(src, dst, rFilter, maxItems, simulateSrc, simulateDst);
    }

    private static int moveSingle0(IItemTransactor src, IItemTransactor dst, IStackFilter filter, int maxItems, boolean simulateSrc, boolean simulateDst) {
        ItemStack potential = src.extract(filter, 1, maxItems, true);
        if (potential.isEmpty()) return 0;
        ItemStack leftOver = dst.insert(potential, false, simulateDst);
        int toTake = potential.getCount() - leftOver.getCount();
        IStackFilter exactFilter = (stack) -> StackUtil.canMerge(stack, potential);
        ItemStack taken = src.extract(exactFilter, toTake, toTake, simulateSrc);
        if (taken.getCount() != toTake) {
            String msg = "One of the two transactors (either src = ";
            msg += src.getClass() + " or dst = " + dst.getClass() + ")";
            msg += " didn't respect the movement flags! ( potential = " + potential;
            msg += ", leftOver = " + leftOver + ", taken = " + taken;
            msg += ", count = " + toTake + " )";
            throw new IllegalStateException(msg);
        }
        return toTake;
    }

    public static IItemInsertable createDroppingTransactor(Level world, Vec3 vec) {
        return (stack, allorNone, simulate) -> {
            if (!simulate) {
                InventoryUtil.drop(world, vec, stack);
            }
            return StackUtil.EMPTY;
        };
    }
}
