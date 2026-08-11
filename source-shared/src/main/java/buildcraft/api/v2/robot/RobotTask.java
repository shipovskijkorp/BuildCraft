package buildcraft.api.v2.robot;

import net.minecraft.resources.ResourceLocation;

public interface RobotTask {
    ResourceLocation typeId();
    RobotTaskResult tick(RobotTaskContext context);
}
