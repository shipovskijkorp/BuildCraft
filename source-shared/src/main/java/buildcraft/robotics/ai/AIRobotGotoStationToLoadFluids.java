package buildcraft.robotics.ai;

import buildcraft.api.core.IFluidFilter;
import buildcraft.api.robots.AIRobot;
import buildcraft.api.robots.DockingStation;
import buildcraft.api.robots.EntityRobotBase;
import buildcraft.robotics.IStationFilter;

public class AIRobotGotoStationToLoadFluids extends AIRobot {
    private IFluidFilter filter;

    public AIRobotGotoStationToLoadFluids(EntityRobotBase robot) {
        super(robot);
    }

    public AIRobotGotoStationToLoadFluids(EntityRobotBase robot, IFluidFilter filter) {
        this(robot);
        this.filter = filter;
    }

    @Override
    public void update() {
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
            return AIRobotLoadFluids.load(robot, station, filter, false) > 0;
        }
    }
}
