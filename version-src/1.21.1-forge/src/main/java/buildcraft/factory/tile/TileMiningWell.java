/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.tile;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.core.SafeTimeTracker;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.core.BCCoreConfig;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.lib.inventory.AutomaticProvidingTransactor;
import buildcraft.lib.misc.BlockUtil;
import buildcraft.lib.misc.CapUtil;
import buildcraft.lib.misc.InventoryUtil;
import buildcraft.lib.mj.MjBatteryReceiver;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

public class TileMiningWell extends TileMiner {
	private final BlockPositionSource blockPosSource = new BlockPositionSource(this.worldPosition);
    private boolean shouldCheck = true;
    private final SafeTimeTracker tracker = new SafeTimeTracker(256);
    public final GameEventListener worldEventListener = new GameEventListener() {
    	@Override
    	public PositionSource getListenerSource() {
    		return blockPosSource;
    	}
    	@Override
    	public int getListenerRadius() {
            int availableDepth = level == null
                ? BCCoreConfig.miningMaxDepth
                : worldPosition.getY() - level.getMinBuildHeight() + 1;
            return Math.max(1, Math.min(BCCoreConfig.miningMaxDepth, availableDepth));
    	}
    	@Override
	public boolean handleGameEvent(ServerLevel serverLevel, Holder<GameEvent> event, GameEvent.Context context, Vec3 pos) {
            if (event != GameEvent.BLOCK_PLACE && event != GameEvent.BLOCK_DESTROY) {
                return false;
            }
            BlockPos eventPos = BlockPos.containing(pos);
            if (eventPos.getX() == worldPosition.getX()
                && eventPos.getY() <= worldPosition.getY()
                && eventPos.getZ() == worldPosition.getZ()) {
                shouldCheck = true;
                return true;
            }
            return false;
    	}
    };

    public TileMiningWell(BlockPos pos, BlockState state) {
    	super(BCFactoryBlocks.ENTITYBLOCKMININGWELL.get(), pos, state);
        caps.addCapabilityInstance(CapUtil.CAP_ITEM_TRANSACTOR, AutomaticProvidingTransactor.INSTANCE, EnumPipePart.VALUES);
    }

    @Override
    protected void mine() {
        if (currentPos != null && canBreak()) {
            shouldCheck = true;
            long target = BlockUtil.computeBlockBreakPower(level, currentPos);
            progress += battery.extractPower(0, target - progress);
            if (progress >= target) {
                progress = 0;
                level.destroyBlockProgress(currentPos.hashCode(), currentPos, -1);
                BlockUtil.breakBlockAndGetDrops(
                    (ServerLevel) level,
                    currentPos,
                    new ItemStack(Items.DIAMOND_PICKAXE),
                    getOwner()
                ).ifPresent(stacks ->
                    stacks.forEach(stack -> InventoryUtil.addToBestAcceptor(level, worldPosition, null, stack))
                );
                nextPos();
            } else {
                if (!level.getBlockState(currentPos).isAir()) {
                    level.destroyBlockProgress(currentPos.hashCode(), currentPos, (int) ((progress * 9) / target));
                }
            }
        } else if (shouldCheck || tracker.markTimeIfDelay(level)) {
            nextPos();
            if (currentPos == null) {
                shouldCheck = false;
            }
        }
    }

    private boolean canBreak() {
        BlockState state = level.getBlockState(currentPos);
        if (state.isAir() || isStandaloneWaterBlock(state)
            || BlockUtil.isUnbreakableBlock(level, currentPos, getOwner())) {
            return false;
        }

        Fluid fluid = BlockUtil.getFluidWithFlowing(level, currentPos);
        return fluid == Fluids.EMPTY || fluid.getFluidType().getViscosity() <= 1000;
    }

    private static boolean isStandaloneWaterBlock(BlockState state) {
        Fluid fluid = BlockUtil.getFluidWithFlowing(state.getBlock());
        return fluid != Fluids.EMPTY && fluid.defaultFluidState().is(FluidTags.WATER);
    }

    private void nextPos() {
    	currentPos = worldPosition;
        while (true) {
        	currentPos = currentPos.below();
            if (level.isOutsideBuildHeight(currentPos)) {
                break;
            }
            if (worldPosition.getY() - currentPos.getY() > BCCoreConfig.miningMaxDepth) {
                break;
            }
            BlockState state = level.getBlockState(currentPos);
            if (canBreak()) {
                updateLength();
                return;
            } else if (isStandaloneWaterBlock(state)) {
                continue;
            } else if (!state.isAir() && state.getBlock() != BCFactoryBlocks.TUBE_BLOCK.get()) {
                break;
            }
        }
        currentPos = null;
        updateLength();
    }

	@Override
	public void onRemove(boolean dropSelf) {
        if (!level.isClientSide) {
            if (currentPos != null) {
                level.destroyBlockProgress(currentPos.hashCode(), currentPos, -1);
            }
        }
		super.onRemove(dropSelf);
	}

	@Override
    protected IMjReceiver createMjReceiver() {
        return new MjBatteryReceiver(battery);
    }
}
