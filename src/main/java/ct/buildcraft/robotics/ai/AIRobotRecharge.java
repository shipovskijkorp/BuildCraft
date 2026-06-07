package ct.buildcraft.robotics.ai;

import ct.buildcraft.api.robots.AIRobot;
import ct.buildcraft.api.robots.EntityRobotBase;

public class AIRobotRecharge extends AIRobot {
    public AIRobotRecharge(EntityRobotBase robot) {
        super(robot);
    }

    @Override
    public int getEnergyCost() {
        return 0;
    }

    @Override
    public void start() {
        robot.getRegistry().releaseResources(robot);
        robot.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        if (robot.getDockingStation() != null && robot.getDockingStation().providesPower()) {
            terminate();
        } else if (robot.getLinkedStation() != null) {
            startDelegateAI(new AIRobotGotoStation(robot, robot.getLinkedStation()));
        } else {
            setSuccess(false);
            terminate();
        }
    }

    @Override
    public void delegateAIEnded(AIRobot ai) {
        if (ai instanceof AIRobotGotoStation && !ai.success()) setSuccess(false);
        terminate();
    }

    @Override
    public void update() {
        if (robot.getEnergy() >= EntityRobotBase.MAX_ENERGY - 500) terminate();
    }
}
