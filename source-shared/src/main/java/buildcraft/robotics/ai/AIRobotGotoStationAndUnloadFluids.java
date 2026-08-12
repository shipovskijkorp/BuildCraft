package buildcraft.robotics.ai;

import buildcraft.robotics.internal.legacy.robots.AIRobot;
import buildcraft.robotics.internal.legacy.robots.EntityRobotBase;

public class AIRobotGotoStationAndUnloadFluids extends AIRobot {
    private boolean requireAcceptAction;

    public AIRobotGotoStationAndUnloadFluids(EntityRobotBase robot) {
        super(robot);
    }

    public AIRobotGotoStationAndUnloadFluids(EntityRobotBase robot, boolean requireAcceptAction) {
        this(robot);
        this.requireAcceptAction = requireAcceptAction;
    }

    @Override
    public void start() {
        startDelegateAI(new AIRobotGotoStationToUnloadFluids(robot, requireAcceptAction));
    }

    @Override
    public void delegateAIEnded(AIRobot ai) {
        if (ai instanceof AIRobotGotoStationToUnloadFluids) {
            if (ai.success()) {
                startDelegateAI(new AIRobotUnloadFluids(robot, requireAcceptAction));
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
