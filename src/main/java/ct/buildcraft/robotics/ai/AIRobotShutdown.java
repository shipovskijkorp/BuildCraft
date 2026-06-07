package ct.buildcraft.robotics.ai;

import ct.buildcraft.api.robots.AIRobot;
import ct.buildcraft.api.robots.EntityRobotBase;
import net.minecraft.world.phys.Vec3;

public class AIRobotShutdown extends AIRobot {
    public AIRobotShutdown(EntityRobotBase robot) {
        super(robot);
    }

    @Override
    public void start() {
        robot.undock();
        robot.setDeltaMovement(new Vec3(robot.getDeltaMovement().x, -0.075D, robot.getDeltaMovement().z));
    }

    @Override
    public void update() {
        // The 1.7.10 robot just keeps falling until blocked. The modern physics layer handles collision/noPhysics.
    }

    @Override
    public int getEnergyCost() {
        return 0;
    }
}
