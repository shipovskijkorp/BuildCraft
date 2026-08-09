package buildcraft.robotics.boards;

import java.util.HashSet;
import java.util.Set;

import buildcraft.api.boards.RedstoneBoardRobot;
import buildcraft.api.boards.RedstoneBoardRobotNBT;
import buildcraft.api.robots.AIRobot;
import buildcraft.api.robots.EntityRobotBase;
import buildcraft.api.robots.DockingStation;
import buildcraft.robotics.BCRoboticsBoards;
import buildcraft.robotics.ai.AIRobotFetchItem;
import buildcraft.robotics.ai.AIRobotGotoSleep;
import buildcraft.robotics.ai.AIRobotGotoStationAndUnload;
import buildcraft.robotics.statements.ActionRobotFilter;

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
        DockingStation station = robot.getLinkedStation();
        if (station == null) {
            // A picker without its home station must not fall back to an unrestricted 250-block search.
            startDelegateAI(new AIRobotGotoSleep(robot));
            return;
        }
        startDelegateAI(new AIRobotFetchItem(robot, 250, ActionRobotFilter.getGateFilter(station), robot.getZoneToWork()));
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
            // GotoStationAndUnload is only started after a failed item search while the robot is carrying stacks.
            // Once it has unloaded successfully there is no current job left, so return to the main dock instead of
            // staying at an arbitrary unload station and waiting for the next search tick to fail again.
            startDelegateAI(new AIRobotGotoSleep(robot));
        } else if (ai instanceof AIRobotGotoSleep) {
            terminate();
        }
    }

    @Override
    public RedstoneBoardRobotNBT getNBTHandler() {
        return BCRoboticsBoards.getByKey("picker").nbt();
    }
}
