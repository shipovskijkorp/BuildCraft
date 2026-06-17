package ct.buildcraft.robotics.ai;

import ct.buildcraft.api.core.IStackFilter;
import ct.buildcraft.api.robots.AIRobot;
import ct.buildcraft.api.robots.DockingStation;
import ct.buildcraft.api.robots.EntityRobotBase;
import ct.buildcraft.robotics.IStationFilter;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Fetches one matching tool from a station-side inventory and equips it as the robot held item. */
public class AIRobotFetchAndEquipItemStack extends AIRobot {
    private IStackFilter filter;
    private int delay;

    public AIRobotFetchAndEquipItemStack(EntityRobotBase robot) {
        super(robot);
    }

    public AIRobotFetchAndEquipItemStack(EntityRobotBase robot, IStackFilter filter) {
        this(robot);
        this.filter = filter;
    }

    @Override
    public void start() {
        if (filter == null) {
            setSuccess(false);
            terminate();
            return;
        }
        if (robot.getDockingStation() == null || !canTakeSingle(robot.getDockingStation(), filter)) {
            startDelegateAI(new AIRobotSearchAndGotoStation(robot, new StationToolFilter(), robot.getZoneToLoadUnload()));
        }
    }

    @Override
    public void update() {
        if (filter == null || robot.getDockingStation() == null) {
            setSuccess(false);
            terminate();
            return;
        }
        if (delay++ > 40) {
            ItemStack stack = takeSingle(robot.getDockingStation(), filter, true);
            if (!stack.isEmpty()) {
                robot.setItemInUse(stack);
                terminate();
            } else {
                delay = 0;
                startDelegateAI(new AIRobotSearchAndGotoStation(robot, new StationToolFilter(), robot.getZoneToLoadUnload()));
            }
        }
    }

    @Override
    public void delegateAIEnded(AIRobot ai) {
        if (ai instanceof AIRobotSearchAndGotoStation && !ai.success()) {
            setSuccess(false);
            terminate();
        }
    }

    private static boolean canTakeSingle(DockingStation station, IStackFilter filter) {
        return !takeSingle(station, filter, false).isEmpty();
    }

    private static ItemStack takeSingle(DockingStation station, IStackFilter filter, boolean doTake) {
        if (station == null || filter == null) return ItemStack.EMPTY;
        Container inventory = station.getItemInput();
        if (inventory == null) return ItemStack.EMPTY;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !filter.matches(stack)) continue;
            ItemStack one = stack.copy();
            one.setCount(1);
            if (doTake) {
                inventory.removeItem(slot, 1);
                inventory.setChanged();
            }
            return one;
        }
        return ItemStack.EMPTY;
    }

    private class StationToolFilter implements IStationFilter {
        @Override
        public boolean matches(DockingStation station) {
            return canTakeSingle(station, filter);
        }
    }
}
