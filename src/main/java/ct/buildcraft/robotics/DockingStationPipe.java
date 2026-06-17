package ct.buildcraft.robotics;

import java.util.Collections;

import ct.buildcraft.api.core.BlockIndex;
import ct.buildcraft.api.robots.DockingStation;
import ct.buildcraft.api.robots.EntityRobotBase;
import ct.buildcraft.api.robots.IRequestProvider;
import ct.buildcraft.api.robots.RobotManager;
import ct.buildcraft.api.statements.StatementSlot;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.item.DyeColor;
import ct.buildcraft.transport.pipe.flow.PipeFlowItems;
import ct.buildcraft.transport.pipe.flow.PipeFlowPower;
import ct.buildcraft.api.transport.IInjectable;
import ct.buildcraft.transport.tile.TilePipeHolder;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public class DockingStationPipe extends DockingStation implements IRequestProvider {
    private TilePipeHolder pipe;

    private final IInjectable injectablePipe = new IInjectable() {
        @Override
        public boolean canInjectItems(Direction from) {
            return getPipe() != null && getPipe().getPipe() != null
                    && getPipe().getPipe().flow instanceof PipeFlowItems
                    && hasItemRoute(from);
        }

        @Override
        public ItemStack injectItem(ItemStack stack, boolean doAdd, Direction from, DyeColor color, double speed) {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            if (getPipe() == null || !(getPipe().getPipe().flow instanceof PipeFlowItems items) || !hasItemRoute(from)) {
                return stack;
            }
            if (doAdd) {
                // In PipeFlowItems the "from" side is the side the stack came from and it is excluded from routing
                // when the item reaches the pipe centre. The robot station sits on side(), so use that side here. The
                // old 1.7 transport used side().getOpposite(), but the 1.19 flow semantics are inverted compared to
                // the old TravelingItem injection path; using the station side prevents picker output from instantly
                // bouncing/dropping instead of travelling into the pipe network.
                items.insertItemsForce(stack.copy(), normalizeOutputSide(from), color, speed);
            }
            return ItemStack.EMPTY;
        }
    };

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

    private Direction normalizeOutputSide(Direction from) {
        return side() != null ? side() : from;
    }

    private boolean hasItemRoute(Direction from) {
        if (getPipe() == null || getPipe().getPipe() == null || !(getPipe().getPipe().flow instanceof PipeFlowItems)) {
            return false;
        }
        Direction blocked = normalizeOutputSide(from);
        for (Direction direction : Direction.values()) {
            if (direction != blocked && getPipe().getPipe().isConnected(direction)) {
                return true;
            }
        }
        return false;
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
    public IInjectable getItemOutput() {
        return getPipe() != null && getPipe().getPipe().flow instanceof PipeFlowItems ? injectablePipe : null;
    }

    @Override
    public Direction getItemOutputSide() {
        return side();
    }

    @Override
    public Container getItemInput() {
        if (getPipe() == null || side() == null || level() == null) return null;
        BlockEntity neighbour = level().getBlockEntity(new net.minecraft.core.BlockPos(x(), y(), z()).relative(side()));
        return neighbour instanceof Container container ? container : null;
    }

    @Override
    public Direction getItemInputSide() {
        return side() == null ? null : side().getOpposite();
    }

    @Override
    public boolean providesPower() {
        return getPipe() != null && getPipe().getPipe() != null && getPipe().getPipe().flow instanceof PipeFlowPower;
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
