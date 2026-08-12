package buildcraft.robotics.ai;

import java.util.HashSet;
import java.util.Set;

import buildcraft.api.core.IZone;
import buildcraft.robotics.internal.legacy.robots.AIRobot;
import buildcraft.robotics.internal.legacy.robots.DockingStation;
import buildcraft.robotics.internal.legacy.robots.EntityRobotBase;
import buildcraft.robotics.IStationFilter;

public class AIRobotSearchAndGotoStation extends AIRobot {
    private IStationFilter filter;
    private IZone zone;
    private final Set<AIRobotSearchStation.StationKey> failedStations = new HashSet<>();
    private DockingStation currentTarget;

    public AIRobotSearchAndGotoStation(EntityRobotBase robot) {
        super(robot);
    }

    public AIRobotSearchAndGotoStation(EntityRobotBase robot, IStationFilter filter, IZone zone) {
        this(robot);
        this.filter = filter;
        this.zone = zone;
    }

    @Override
    public void start() {
        searchNextStation();
    }

    private void searchNextStation() {
        startDelegateAI(new AIRobotSearchStation(robot, filter, zone, failedStations));
    }

    @Override
    public void delegateAIEnded(AIRobot ai) {
        if (ai instanceof AIRobotSearchStation search) {
            if (ai.success()) {
                currentTarget = search.targetStation;
                startDelegateAI(new AIRobotGotoStation(robot, currentTarget));
            } else {
                setSuccess(false);
                terminate();
            }
        } else if (ai instanceof AIRobotGotoStation) {
            if (ai.success()) {
                setSuccess(true);
                terminate();
            } else {
                if (currentTarget != null) {
                    failedStations.add(AIRobotSearchStation.StationKey.of(currentTarget));
                }
                currentTarget = null;
                searchNextStation();
            }
        }
    }
}
