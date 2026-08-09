package buildcraft.robotics.ai;

import buildcraft.api.robots.AIRobot;
import buildcraft.api.robots.DockingStation;
import buildcraft.api.robots.EntityRobotBase;
import buildcraft.robotics.IStationFilter;

public class AIRobotGotoStationToUnloadFluids extends AIRobot {
    private boolean requireAcceptAction;

    public AIRobotGotoStationToUnloadFluids(EntityRobotBase robot) {
        super(robot);
    }

    public AIRobotGotoStationToUnloadFluids(EntityRobotBase robot, boolean requireAcceptAction) {
        this(robot);
        this.requireAcceptAction = requireAcceptAction;
    }

    @Override
    public void start() {
        startDelegateAI(new AIRobotSearchAndGotoStation(robot, new StationFilter(), robot.getZoneToLoadUnload()));
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
            return AIRobotUnloadFluids.unload(robot, station, false, requireAcceptAction) > 0;
        }
    }
}
