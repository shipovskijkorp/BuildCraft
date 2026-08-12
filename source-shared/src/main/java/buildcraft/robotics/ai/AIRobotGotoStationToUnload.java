package buildcraft.robotics.ai;

import buildcraft.robotics.internal.legacy.robots.AIRobot;
import buildcraft.robotics.internal.legacy.robots.DockingStation;
import buildcraft.robotics.internal.legacy.robots.EntityRobotBase;
import buildcraft.robotics.IStationFilter;

public class AIRobotGotoStationToUnload extends AIRobot {
    private boolean requireAcceptAction;

    public AIRobotGotoStationToUnload(EntityRobotBase robot) {
        super(robot);
    }

    public AIRobotGotoStationToUnload(EntityRobotBase robot, boolean requireAcceptAction) {
        this(robot);
        this.requireAcceptAction = requireAcceptAction;
    }

    @Override
    public void start() {
        startDelegateAI(new AIRobotSearchAndGotoStation(robot, new StationInventory(), robot.getZoneToLoadUnload()));
    }

    @Override
    public void delegateAIEnded(AIRobot ai) {
        if (ai instanceof AIRobotSearchAndGotoStation) {
            setSuccess(ai.success());
            terminate();
        }
    }

    private class StationInventory implements IStationFilter {
        @Override
        public boolean matches(DockingStation station) {
            return AIRobotUnload.unload(robot, station, false, requireAcceptAction);
        }
    }
}
