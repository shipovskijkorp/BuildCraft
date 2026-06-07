package ct.buildcraft.robotics.ai;

import ct.buildcraft.api.robots.AIRobot;
import ct.buildcraft.api.robots.DockingStation;
import ct.buildcraft.api.robots.EntityRobotBase;
import ct.buildcraft.api.transport.IInjectable;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public class AIRobotUnload extends AIRobot {
    private int waitedCycles;

    public AIRobotUnload(EntityRobotBase robot) {
        super(robot);
    }

    @Override
    public void update() {
        waitedCycles++;
        if (waitedCycles > 40) {
            if (unload(robot, robot.getDockingStation(), true)) {
                waitedCycles = 0;
            } else {
                setSuccess(!robot.containsItems());
                terminate();
            }
        }
    }

    public static boolean unload(EntityRobotBase robot, DockingStation station, boolean doUnload) {
        if (robot == null || station == null) return false;
        for (int slot = 0; slot < robot.getContainerSize(); slot++) {
            ItemStack stack = robot.getItem(slot);
            if (stack.isEmpty()) continue;
            int before = stack.getCount();
            ItemStack remaining = offer(station, stack, doUnload);
            int used = before - remaining.getCount();
            if (used > 0) {
                if (doUnload) robot.removeItem(slot, used);
                return true;
            }
        }
        return false;
    }

    private static ItemStack offer(DockingStation station, ItemStack stack, boolean doAdd) {
        IInjectable output = station.getItemOutput();
        Direction side = station.getItemOutputSide();
        if (output != null && side != null && output.canInjectItems(side)) {
            ItemStack remaining = output.injectItem(stack.copy(), doAdd, side, null, 0.08D);
            if (remaining.getCount() < stack.getCount()) {
                return remaining;
            }
        }
        Container container = station.getItemInput();
        if (container != null) {
            return insertIntoContainer(container, stack, doAdd);
        }
        return stack;
    }

    private static ItemStack insertIntoContainer(Container container, ItemStack stack, boolean doAdd) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < container.getContainerSize() && !remaining.isEmpty(); i++) {
            ItemStack existing = container.getItem(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameTags(existing, remaining) && existing.isStackable()) {
                int limit = Math.min(existing.getMaxStackSize(), container.getMaxStackSize());
                int move = Math.min(limit - existing.getCount(), remaining.getCount());
                if (move > 0) {
                    if (doAdd) {
                        existing.grow(move);
                        container.setChanged();
                    }
                    remaining.shrink(move);
                }
            }
        }
        for (int i = 0; i < container.getContainerSize() && !remaining.isEmpty(); i++) {
            if (container.getItem(i).isEmpty() && container.canPlaceItem(i, remaining)) {
                int move = Math.min(Math.min(remaining.getMaxStackSize(), container.getMaxStackSize()), remaining.getCount());
                if (doAdd) {
                    ItemStack inserted = remaining.copy();
                    inserted.setCount(move);
                    container.setItem(i, inserted);
                    container.setChanged();
                }
                remaining.shrink(move);
            }
        }
        return remaining;
    }

    @Override
    public int getEnergyCost() {
        return 10;
    }
}
