package buildcraft.robotics.ai;

import buildcraft.robotics.internal.legacy.robots.AIRobot;
import buildcraft.robotics.internal.legacy.robots.DockingStation;
import buildcraft.robotics.internal.legacy.robots.IRobotRegistry;
import buildcraft.robotics.internal.legacy.robots.EntityRobotBase;

public class AIRobotGotoSleep extends AIRobot {
    public AIRobotGotoSleep(EntityRobotBase robot) {
        super(robot);
    }

    @Override
    public void start() {
        IRobotRegistry registry = robot.getRegistry();
        if (registry != null) {
            registry.releaseResources(robot);
        }

        DockingStation linkedStation = robot.getLinkedStation();
        if (linkedStation == null) {
            setSuccess(false);
            terminate();
            return;
        }
        startDelegateAI(new AIRobotGotoStation(robot, linkedStation));
    }

    @Override
    public void delegateAIEnded(AIRobot ai) {
        if (ai instanceof AIRobotGotoStation) {
            if (ai.success()) {
                startDelegateAI(new AIRobotSleep(robot));
            } else {
                setSuccess(false);
                terminate();
            }
        } else if (ai instanceof AIRobotSleep) {
            terminate();
        }
    }
}
