package buildcraft.robotics.boards;

import buildcraft.robotics.internal.legacy.boards.RedstoneBoardRobot;
import buildcraft.robotics.internal.legacy.boards.RedstoneBoardRobotNBT;
import buildcraft.api.core.IStackFilter;
import buildcraft.robotics.internal.legacy.robots.AIRobot;
import buildcraft.robotics.internal.legacy.robots.EntityRobotBase;
import buildcraft.robotics.BCRoboticsBoards;
import buildcraft.robotics.ai.AIRobotGotoSleep;
import buildcraft.robotics.ai.AIRobotGotoStationAndLoad;
import buildcraft.robotics.ai.AIRobotGotoStationAndUnload;
import buildcraft.robotics.ai.AIRobotLoad;
import buildcraft.robotics.statements.ActionRobotFilter;

public class BoardRobotCarrier extends RedstoneBoardRobot {
    public BoardRobotCarrier(EntityRobotBase robot) {
        super(robot);
    }

    @Override
    public RedstoneBoardRobotNBT getNBTHandler() {
        return BCRoboticsBoards.getByKey("carrier").nbt();
    }

    @Override
    public void update() {
        if (!robot.containsItems()) {
            IStackFilter filter = ActionRobotFilter.getGateFilter(robot.getLinkedStation());
            startDelegateAI(new AIRobotGotoStationAndLoad(robot, filter, AIRobotLoad.ANY_QUANTITY));
        } else {
            startDelegateAI(new AIRobotGotoStationAndUnload(robot, true));
        }
    }

    @Override
    public void delegateAIEnded(AIRobot ai) {
        if (ai instanceof AIRobotGotoStationAndLoad) {
            if (!ai.success()) {
                startDelegateAI(new AIRobotGotoSleep(robot));
            }
        } else if (ai instanceof AIRobotGotoStationAndUnload) {
            if (!ai.success()) {
                startDelegateAI(new AIRobotGotoSleep(robot));
            }
        } else if (ai instanceof AIRobotGotoSleep) {
            terminate();
        }
    }
}
