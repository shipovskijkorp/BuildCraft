package ct.buildcraft.robotics.boards;

import java.util.HashSet;
import java.util.Set;

import ct.buildcraft.api.boards.RedstoneBoardRobot;
import ct.buildcraft.api.boards.RedstoneBoardRobotNBT;
import ct.buildcraft.api.robots.AIRobot;
import ct.buildcraft.api.robots.EntityRobotBase;
import ct.buildcraft.robotics.BCRoboticsBoards;
import ct.buildcraft.robotics.ai.AIRobotFetchItem;
import ct.buildcraft.robotics.ai.AIRobotGotoSleep;
import ct.buildcraft.robotics.ai.AIRobotGotoStationAndUnload;
import ct.buildcraft.robotics.statements.ActionRobotFilter;

/** 1.7.10 BuildCraft picker board port. Picks dropped item entities, unloads them, then sleeps. */
public class BoardRobotPicker extends RedstoneBoardRobot {
    public static final Set<Integer> targettedItems = new HashSet<>();

    public BoardRobotPicker(EntityRobotBase robot) {
        super(robot);
    }

    public static void onServerStart() {
        targettedItems.clear();
    }

    private void fetchNewItem() {
        startDelegateAI(new AIRobotFetchItem(robot, 250, ActionRobotFilter.getGateFilter(robot.getLinkedStation()), robot.getZoneToWork()));
    }

    @Override
    public void update() {
        fetchNewItem();
    }

    @Override
    public void delegateAIEnded(AIRobot ai) {
        if (ai instanceof AIRobotFetchItem) {
            if (ai.success()) {
                fetchNewItem();
            } else if (robot.containsItems()) {
                startDelegateAI(new AIRobotGotoStationAndUnload(robot));
            } else {
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

    @Override
    public RedstoneBoardRobotNBT getNBTHandler() {
        return BCRoboticsBoards.getByKey("picker").nbt();
    }
}
