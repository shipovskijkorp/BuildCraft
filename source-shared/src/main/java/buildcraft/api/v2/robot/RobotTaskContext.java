package buildcraft.api.v2.robot;

import buildcraft.api.v2.permission.AutomationActor;
import java.util.Objects;
import net.minecraft.world.level.Level;

public record RobotTaskContext(Level level, RobotHandle robot, AutomationActor actor) {
    public RobotTaskContext {
        Objects.requireNonNull(level, "level"); Objects.requireNonNull(robot, "robot"); Objects.requireNonNull(actor, "actor");
    }
}
