package ct.buildcraft.robotics;

import java.util.Collections;

import ct.buildcraft.api.core.BlockIndex;
import ct.buildcraft.api.robots.DockingStation;
import ct.buildcraft.api.robots.EntityRobotBase;
import ct.buildcraft.api.robots.IRequestProvider;
import ct.buildcraft.api.robots.RobotManager;
import ct.buildcraft.api.statements.StatementSlot;
import ct.buildcraft.transport.tile.TilePipeHolder;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public class DockingStationPipe extends DockingStation implements IRequestProvider {
    private TilePipeHolder pipe;

    public DockingStationPipe() {
    }

    public DockingStationPipe(TilePipeHolder pipe, Direction side) {
        super(new BlockIndex(pipe.getPipePos()), side);
        this.pipe = pipe;
        setLevel(pipe.getPipeWorld());
    }

    public TilePipeHolder getPipe() {
        if (pipe == null && level() != null) {
            if (level().getBlockEntity(new net.minecraft.core.BlockPos(x(), y(), z())) instanceof TilePipeHolder holder) {
                pipe = holder;
            }
        }
        if (pipe == null && level() != null && !level().isClientSide && RobotManager.registryProvider != null) {
            RobotManager.registryProvider.getRegistry(level()).removeStation(this);
        }
        return pipe;
    }

    @Override
    public Iterable<StatementSlot> getActiveActions() {
        return Collections.emptyList();
    }

    @Override
    public boolean isInitialized() {
        return getPipe() != null && getPipe().getPipe() != null;
    }

    @Override
    public boolean take(EntityRobotBase robot) {
        if (getPipe() == null) return false;
        boolean result = super.take(robot);
        if (result) {
            pipe.scheduleRenderUpdate();
        }
        return result;
    }

    @Override
    public boolean takeAsMain(EntityRobotBase robot) {
        if (getPipe() == null) return false;
        boolean result = super.takeAsMain(robot);
        if (result) {
            pipe.scheduleRenderUpdate();
        }
        return result;
    }

    @Override
    public void unsafeRelease(EntityRobotBase robot) {
        super.unsafeRelease(robot);
        if (getPipe() != null) {
            pipe.scheduleRenderUpdate();
        }
    }

    @Override
    public int getRequestsCount() {
        return 0;
    }

    @Override
    public ItemStack getRequest(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack offerItem(int slot, ItemStack stack) {
        return stack;
    }

    @Override
    public IRequestProvider getRequestProvider() {
        return this;
    }

    @Override
    public void onChunkUnload() {
        pipe = null;
    }
}
