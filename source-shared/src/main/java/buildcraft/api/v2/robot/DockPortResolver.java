package buildcraft.api.v2.robot;

import java.util.Optional;

/** Resolves an addon-defined view for a BuildCraft docking station. */
@FunctionalInterface
public interface DockPortResolver<T> {
    Optional<T> resolve(RobotDockContext context);
}
