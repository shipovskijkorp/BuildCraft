package buildcraft.robotics.ai;

import buildcraft.api.robots.AIRobot;
import buildcraft.api.robots.DockingStation;
import buildcraft.api.robots.EntityRobotBase;
import buildcraft.robotics.IStationFilter;
import net.minecraft.world.phys.Vec3;

public class AIRobotRecharge extends AIRobot {
    public AIRobotRecharge(EntityRobotBase robot) {
        super(robot);
    }

    @Override
    public void start() {
        robot.getRegistry().releaseResources(robot);
        robot.setDeltaMovement(Vec3.ZERO);

        startDelegateAI(new AIRobotSearchAndGotoStation(robot, new IStationFilter() {
            @Override
            public boolean matches(DockingStation station) {
                return station.providesPower();
            }
        }, null));
    }

    @Override
    public int getEnergyCost() {
        return 0;
    }

    @Override
    public void update() {
        if (robot.getEnergy() >= EntityRobotBase.MAX_ENERGY - 500) {
            terminate();
        }
    }

    @Override
    public void delegateAIEnded(AIRobot ai) {
        if (ai instanceof AIRobotSearchAndGotoStation && !ai.success()) {
            setSuccess(false);
            terminate();
        }
    }
}
