package buildcraft.robotics.ai;

import buildcraft.api.core.IStackFilter;
import buildcraft.api.robots.AIRobot;
import buildcraft.api.robots.EntityRobotBase;

public class AIRobotGotoStationAndLoad extends AIRobot {
    private IStackFilter filter;
    private int quantity;
    private boolean ignoreStationZone;

    public AIRobotGotoStationAndLoad(EntityRobotBase robot) {
        super(robot);
    }

    public AIRobotGotoStationAndLoad(EntityRobotBase robot, IStackFilter filter, int quantity) {
        this(robot, filter, quantity, false);
    }

    public AIRobotGotoStationAndLoad(EntityRobotBase robot, IStackFilter filter, int quantity, boolean ignoreStationZone) {
        this(robot);
        this.filter = filter;
        this.quantity = quantity;
        this.ignoreStationZone = ignoreStationZone;
    }

    @Override
    public void start() {
        startDelegateAI(new AIRobotGotoStationToLoad(robot, filter, quantity, ignoreStationZone));
    }

    @Override
    public void delegateAIEnded(AIRobot ai) {
        if (ai instanceof AIRobotGotoStationToLoad) {
            if (filter != null && ai.success()) {
                startDelegateAI(new AIRobotLoad(robot, filter, quantity));
            } else {
                setSuccess(false);
                terminate();
            }
        } else if (ai instanceof AIRobotLoad) {
            setSuccess(ai.success());
            terminate();
        }
    }
}
