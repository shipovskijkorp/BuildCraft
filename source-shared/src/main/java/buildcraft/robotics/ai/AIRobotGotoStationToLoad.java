package buildcraft.robotics.ai;

import buildcraft.lib.internal.core.IStackFilter;
import buildcraft.robotics.internal.legacy.robots.AIRobot;
import buildcraft.robotics.internal.legacy.robots.DockingStation;
import buildcraft.robotics.internal.legacy.robots.EntityRobotBase;
import buildcraft.robotics.IStationFilter;

public class AIRobotGotoStationToLoad extends AIRobot {
    private IStackFilter filter;
    private int quantity;
    private boolean ignoreStationZone;

    public AIRobotGotoStationToLoad(EntityRobotBase robot) {
        super(robot);
    }

    public AIRobotGotoStationToLoad(EntityRobotBase robot, IStackFilter filter, int quantity) {
        this(robot, filter, quantity, false);
    }

    public AIRobotGotoStationToLoad(EntityRobotBase robot, IStackFilter filter, int quantity, boolean ignoreStationZone) {
        this(robot);
        this.filter = filter;
        this.quantity = quantity;
        this.ignoreStationZone = ignoreStationZone;
    }

    @Override
    public void update() {
        startDelegateAI(new AIRobotSearchAndGotoStation(robot, new StationFilter(), ignoreStationZone ? null : robot.getZoneToLoadUnload()));
    }

    @Override
    public void delegateAIEnded(AIRobot ai) {
        if (ai instanceof AIRobotSearchAndGotoStation) {
            setSuccess(ai.success());
            terminate();
        }
    }

    private class StationFilter implements IStationFilter {
        @Override
        public boolean matches(DockingStation station) {
            return AIRobotLoad.load(robot, station, filter, quantity, false);
        }
    }
}
