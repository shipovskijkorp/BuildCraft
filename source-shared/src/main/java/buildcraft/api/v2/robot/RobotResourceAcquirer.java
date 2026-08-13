package buildcraft.api.v2.robot;

import java.util.Optional;
import net.minecraft.world.level.Level;

/** Runtime strategy used by a registered robot resource type to acquire and release addon-owned resources. */
@FunctionalInterface
public interface RobotResourceAcquirer<R extends RobotResource> {
    Optional<RobotResourceLease> acquire(Level level, long robotId, R resource, long amount);
}
