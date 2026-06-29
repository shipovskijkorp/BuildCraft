package ct.buildcraft.robotics.ai;

import ct.buildcraft.api.robots.AIRobot;
import ct.buildcraft.api.robots.EntityRobotBase;

public class AIRobotGotoStationAndUnloadFluids extends AIRobot {
    public AIRobotGotoStationAndUnloadFluids(EntityRobotBase robot) {
        super(robot);
    }

    @Override
    public void start() {
        startDelegateAI(new AIRobotGotoStationToUnloadFluids(robot));
    }

    @Override
    public void delegateAIEnded(AIRobot ai) {
        if (ai instanceof AIRobotGotoStationToUnloadFluids) {
            if (ai.success()) {
                startDelegateAI(new AIRobotUnloadFluids(robot));
            } else {
                setSuccess(false);
                terminate();
            }
        } else if (ai instanceof AIRobotUnloadFluids) {
            setSuccess(ai.success());
            terminate();
        }
    }
}
