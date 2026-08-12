package buildcraft.robotics.internal.api2;

import buildcraft.api.v2.BuildCraftApi;
import buildcraft.api.v2.BuildCraftServices;
import buildcraft.api.v2.automation.AutomationRequest;
import buildcraft.api.v2.automation.AutomationResult;
import buildcraft.api.v2.permission.AutomationActor;
import buildcraft.lib.misc.FakePlayerProvider;
import buildcraft.robotics.entity.EntityRobot;
import buildcraft.robotics.internal.legacy.robots.EntityRobotBase;
import com.mojang.authlib.GameProfile;
import net.minecraft.resources.ResourceLocation;

/** Common owner identity and automation-policy bridge for classic robot AIs. */
public final class RobotAutomationSupport {
    private RobotAutomationSupport() {}

    public static AutomationActor actor(EntityRobotBase robot) {
        if (robot instanceof EntityRobot entity) {
            GameProfile owner = entity.getOwnerProfile();
            if (owner != null && owner.getId() != null && !owner.getId().equals(FakePlayerProvider.NULL_PROFILE.getId())) {
                return AutomationActor.machineOwner(owner.getId(), owner.getName(), new ResourceLocation("buildcraft", "robot"));
            }
        }
        return AutomationActor.system(new ResourceLocation("buildcraft", "robot"));
    }

    public static boolean permits(AutomationRequest request) {
        AutomationResult result = BuildCraftApi.service(BuildCraftServices.AUTOMATION).execute(request);
        return result.status() != AutomationResult.Status.DENIED && result.status() != AutomationResult.Status.FAILED;
    }
}
