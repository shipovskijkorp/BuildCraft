package ct.buildcraft.robotics.ai;

import ct.buildcraft.api.robots.AIRobot;
import ct.buildcraft.api.robots.EntityRobotBase;

public class AIRobotGotoSleep extends AIRobot {
    public AIRobotGotoSleep(EntityRobotBase robot) {
        super(robot);
    }

    @Override
    public void start() {
        robot.getRegistry().releaseResources(robot);
        if (robot.getLinkedStation() == null) {
            setSuccess(false);
            terminate();
            return;
        }
        startDelegateAI(new AIRobotGotoStation(robot, robot.getLinkedStation()));
    }

    @Override
    public void delegateAIEnded(AIRobot ai) {
        if (ai instanceof AIRobotGotoStation) {
            startDelegateAI(new AIRobotSleep(robot));
        } else if (ai instanceof AIRobotSleep) {
            terminate();
        }
    }
}
