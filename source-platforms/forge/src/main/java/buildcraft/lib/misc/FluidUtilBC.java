/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.misc;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import buildcraft.lib.internal.debug.BCLog;
import buildcraft.lib.internal.core.IFluidFilter;
import buildcraft.lib.internal.core.IFluidHandlerAdv;
import buildcraft.compat.CompatCapTransfromer;
import buildcraft.core.BCCoreItems;
import buildcraft.core.item.ItemFragileFluidContainer;
import buildcraft.lib.fluid.FluidCompatRegistry;
import buildcraft.lib.fluid.Tank;
import buildcraft.lib.fluid.TankManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.ItemHandlerHelper;

public class FluidUtilBC {

    public static void pushFluidAround(BlockGetter world, BlockPos pos, Tank tank) {
        for (Direction side : Direction.values()) {
            FluidStack available = tank.drainInternal(tank.getFluidAmount(), FluidAction.SIMULATE);
            if (available.isEmpty() || available.getAmount() <= 0) {
                return;
            }

            BlockEntity target = world.getBlockEntity(pos.relative(side));
            if (target == null) {
                continue;
            }
            IFluidHandler handler = CompatCapTransfromer.INSTANCE
                .getCap(target, CapUtil.CAP_FLUIDS, side.getOpposite())
                .orElse(null);
            if (handler == null) {
                continue;
            }

            int accepted = Math.min(available.getAmount(), Math.max(0,
                handler.fill(available.copy(), FluidAction.SIMULATE)));
            if (accepted <= 0) {
                continue;
            }

            // Drain the source before filling the destination. The old order could create fluid when the source
            // changed between simulation and execution.
            FluidStack drained = tank.drainInternal(new FluidStack(available, accepted), FluidAction.EXECUTE);
            if (drained.isEmpty() || drained.getAmount() <= 0) {
                continue;
            }

            int actuallyAccepted = Math.min(drained.getAmount(), Math.max(0,
                handler.fill(drained.copy(), FluidAction.EXECUTE)));
            if (actuallyAccepted < drained.getAmount()) {
                FluidStack remainder = new FluidStack(drained, drained.getAmount() - actuallyAccepted);
                int restored = tank.fillInternal(remainder, FluidAction.EXECUTE);
                if (restored != remainder.getAmount()) {
                    BCLog.logger.error("Failed to roll back {} mB after a destination fluid handler accepted only {} mB",
                        remainder.getAmount(), actuallyAccepted);
                }
            }
        }
    }

    public static List<FluidStack> mergeSameFluids(List<FluidStack> fluids) {
        List<FluidStack> stacks = new ArrayList<>();
        fluids.forEach(toAdd -> {
            boolean found = false;
            for (FluidStack stack : stacks) {
                if (FluidCompatRegistry.areEquivalent(stack, toAdd)) {
                    stack.setAmount(stack.getAmount() + toAdd.getAmount());
                    found = true;
                }
            }
            if (!found&&!toAdd.isEmpty()) {
                stacks.add(toAdd.copy());
            }
        });
        return stacks;
    }

    public static boolean areFluidStackEqual(FluidStack a, FluidStack b) {
        return (a == FluidStack.EMPTY && b == FluidStack.EMPTY) || (a != FluidStack.EMPTY && FluidCompatRegistry.areEquivalent(a, b) && a.getAmount() == b.getAmount());
    }

    public static boolean areFluidsEqual(Fluid a, Fluid b) {
        if (a == Fluids.EMPTY || b == Fluids.EMPTY) {
            return a == b;
        }
        return FluidCompatRegistry.areEquivalent(a, b);
    }

    /** @return The fluidstack that was moved, or null if no fluid was moved. */
    @Nullable
    public static FluidStack move(IFluidHandler from, IFluidHandler to) {
        return move(from, to, Integer.MAX_VALUE);
    }

    /** @param max The maximum amount of fluid to move.
     * @return The fluidstack that was moved, or null if no fluid was moved. */
    @Nullable
    public static FluidStack move(IFluidHandler from, IFluidHandler to, int max) {
        if (from == null || to == null || max <= 0) {
            return FluidStack.EMPTY;
        }

        FluidStack potential;
        if (from instanceof IFluidHandlerAdv) {
            IFluidFilter filter = fluid -> to.fill(fluid, FluidAction.SIMULATE) > 0;
            potential = ((IFluidHandlerAdv) from).drain(filter, max, FluidAction.SIMULATE);
        } else {
            potential = from.drain(max, FluidAction.SIMULATE);
        }
        if (potential.isEmpty() || potential.getAmount() <= 0) {
            return FluidStack.EMPTY;
        }

        int accepted = Math.min(potential.getAmount(), Math.max(0,
            to.fill(potential.copy(), FluidAction.SIMULATE)));
        if (accepted <= 0) {
            return FluidStack.EMPTY;
        }

        FluidStack requested = new FluidStack(potential, accepted);
        FluidStack stillAvailable = from.drain(requested.copy(), FluidAction.SIMULATE);
        if (!sameFluidAndAmount(requested, stillAvailable)) {
            return FluidStack.EMPTY;
        }

        // Source-first execution removes the old fill-before-drain duplication path. If the destination accepts less
        // than it simulated, return the remainder to the source.
        FluidStack drained = from.drain(requested.copy(), FluidAction.EXECUTE);
        if (drained.isEmpty() || drained.getAmount() <= 0) {
            return FluidStack.EMPTY;
        }
        if (!FluidCompatRegistry.areEquivalent(requested, drained)) {
            restoreFluid(from, drained, "source returned a different fluid");
            return FluidStack.EMPTY;
        }

        int actuallyAccepted;
        try {
            actuallyAccepted = to.fill(drained.copy(), FluidAction.EXECUTE);
        } catch (RuntimeException exception) {
            restoreFluid(from, drained, "destination threw while accepting fluid");
            BCLog.logger.warn("A destination fluid handler threw while BuildCraft was moving fluid", exception);
            return FluidStack.EMPTY;
        }
        actuallyAccepted = Math.min(drained.getAmount(), Math.max(0, actuallyAccepted));

        if (actuallyAccepted < drained.getAmount()) {
            FluidStack remainder = new FluidStack(drained, drained.getAmount() - actuallyAccepted);
            restoreFluid(from, remainder, "destination accepted less than it simulated");
        }
        return actuallyAccepted <= 0 ? FluidStack.EMPTY : new FluidStack(drained, actuallyAccepted);
    }

    private static boolean sameFluidAndAmount(FluidStack expected, FluidStack actual) {
        return !expected.isEmpty() && !actual.isEmpty()
            && expected.getAmount() == actual.getAmount()
            && FluidCompatRegistry.areEquivalent(expected, actual);
    }

    private static void restoreFluid(IFluidHandler handler, FluidStack fluid, String reason) {
        if (fluid.isEmpty() || fluid.getAmount() <= 0) {
            return;
        }
        int restored;
        try {
            if (handler instanceof Tank) {
                restored = ((Tank) handler).fillInternal(fluid.copy(), FluidAction.EXECUTE);
            } else if (handler instanceof TankManager) {
                restored = restoreToTankManager((TankManager) handler, fluid);
            } else {
                restored = handler.fill(fluid.copy(), FluidAction.EXECUTE);
            }
            restored = Math.min(fluid.getAmount(), Math.max(0, restored));
        } catch (RuntimeException exception) {
            BCLog.logger.error("Failed to roll back fluid after {}", reason, exception);
            return;
        }
        if (restored != fluid.getAmount()) {
            BCLog.logger.error("Restored only {} of {} mB after {}", restored, fluid.getAmount(), reason);
        }
    }

    private static int restoreToTankManager(TankManager manager, FluidStack fluid) {
        FluidStack remaining = fluid.copy();
        int restored = 0;

        // Prefer the tank that already contains this fluid, which is normally the exact tank the transfer drained.
        for (Tank tank : manager) {
            if (remaining.isEmpty() || tank.getFluid().isEmpty()
                || !FluidCompatRegistry.areEquivalent(tank.getFluid(), remaining)) {
                continue;
            }
            int filled = tank.fillInternal(remaining, FluidAction.EXECUTE);
            restored += filled;
            remaining.shrink(filled);
        }
        for (Tank tank : manager) {
            if (remaining.isEmpty() || !tank.getFluid().isEmpty()) {
                continue;
            }
            int filled = tank.fillInternal(remaining, FluidAction.EXECUTE);
            restored += filled;
            remaining.shrink(filled);
        }
        return restored;
    }

    public static boolean onTankActivated(Player player, BlockPos pos, InteractionHand hand,
        IFluidHandler fluidHandler) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            return false;
        }
        //? if <1.20 {
        Level world = player.level;
        //?} else {
        /*?
        Level world = player.level();
        ?*/
        //?}

        // BucketItem capability wrappers on Forge 43/47 do not reliably update getContainer() for modded
        // buckets. Treat buckets as atomic 1000 mB containers so custom BuildCraft oil buckets obey the same
        // consume/return-empty-bucket contract as water and lava buckets.
        if (held.getItem() instanceof BucketItem) {
            FluidStack contained = FluidUtil.getFluidContained(held.copy()).orElse(FluidStack.EMPTY);
            if (!contained.isEmpty()) {
                FluidStack bucketFluid = new FluidStack(contained, FluidType.BUCKET_VOLUME);
                int accepted = fluidHandler.fill(bucketFluid.copy(), FluidAction.SIMULATE);
                if (accepted != FluidType.BUCKET_VOLUME) {
                    return true;
                }
                if (!world.isClientSide) {
                    int filled = fluidHandler.fill(bucketFluid.copy(), FluidAction.EXECUTE);
                    if (filled != FluidType.BUCKET_VOLUME) {
                        throw new IllegalStateException("Simulated a full bucket fill but executed " + filled + " mB");
                    }
                    SoundUtil.playBucketEmpty(world, pos, bucketFluid);
                    consumeOneAndGive(player, hand, held, new ItemStack(Items.BUCKET));
                }
                return true;
            }
        }

        if (held.is(Items.BUCKET)) {
            if (player.isCreative()) {
                return world.isClientSide;
            }
            FluidStack simulated = fluidHandler.drain(FluidType.BUCKET_VOLUME, FluidAction.SIMULATE);
            if (simulated.isEmpty() || simulated.getAmount() != FluidType.BUCKET_VOLUME
                || simulated.getFluid().getBucket() == Items.AIR) {
                return true;
            }
            if (!world.isClientSide) {
                FluidStack requested = new FluidStack(simulated, FluidType.BUCKET_VOLUME);
                FluidStack drained = fluidHandler.drain(requested, FluidAction.EXECUTE);
                if (drained.isEmpty() || drained.getAmount() != FluidType.BUCKET_VOLUME
                    || !FluidCompatRegistry.areEquivalent(simulated, drained)) {
                    throw new IllegalStateException("Simulated a full bucket drain but execution returned " + drained);
                }
                SoundUtil.playBucketFill(world, pos, drained);
                consumeOneAndGive(player, hand, held, new ItemStack(drained.getFluid().getBucket()));
            }
            return true;
        }

        boolean replace = !player.isCreative();
        boolean single = held.getCount() == 1;

        // Always transact through an isolated one-item copy. Modern bucket/item capability wrappers are allowed to
        // replace their container stack while drain(EXECUTE) runs; doing that directly against the player's held
        // stack can invalidate the live wrapper before we read getContainer(), which made survival bucket/shard
        // insertion fail even though the same transfer worked with the creative-mode copy path.
        ItemStack transferStack = held.copy();
        transferStack.setCount(1);
        IFluidHandlerItem flItem = FluidUtil.getFluidHandler(transferStack).orElse(null);
        if (flItem == null) {
            return false;
        }
        if (world.isClientSide) {
            return true;
        }
        boolean changed = true;
        FluidStack moved;
        if ((moved = FluidUtilBC.move(flItem, fluidHandler)) != FluidStack.EMPTY) {
            SoundUtil.playBucketEmpty(world, pos, moved);
        } else if (replace && (moved = FluidUtilBC.move(fluidHandler, flItem)) != FluidStack.EMPTY) {
            // In creative mode the temporary item handler is discarded, so draining the tank here would delete fluid
            // without changing the player's held container.
            SoundUtil.playBucketFill(world, pos, moved);
        } else {
            changed = false;
        }

        if (changed && replace) {
            if (single) {
                // if it was the single item, replace with changed one
                player.setItemInHand(hand, flItem.getContainer());
            } else {
                // if it was part of stack, shrink stack and give / drop the new one
                held.shrink(1);
                ItemHandlerHelper.giveItemToPlayer(player, flItem.getContainer());
            }
//            player.inventoryContainer.detectAndSendChanges();
        }
        // BC8 consumed the interaction as soon as the held item was a fluid container, even when the target tank
        // rejected its contents. Returning PASS here lets BucketItem continue with world placement, so water/lava/oil
        // can be placed into/through BuildCraft machine blocks after a rejected transfer. Keep the original claim
        // semantics while only changing inventory/tank state when an actual transfer occurred.
        return true;
    }

    private static void consumeOneAndGive(Player player, InteractionHand hand, ItemStack held, ItemStack result) {
        if (player.isCreative()) {
            return;
        }
        if (held.getCount() == 1) {
            player.setItemInHand(hand, result);
            return;
        }
        held.shrink(1);
        if (!result.isEmpty()) {
            ItemHandlerHelper.giveItemToPlayer(player, result);
        }
    }
    
    public static ItemStack getFragileFluid(FluidStack fluid) {
    //	if(fluid.isEmpty())
 //   		return ItemStack.EMPTY;
    	ItemStack item = new ItemStack(BCCoreItems.FRAGILE_FLUID_SHARD.get());
    	ItemFragileFluidContainer.setFluid(item, fluid);
    	return item;
    }
}
