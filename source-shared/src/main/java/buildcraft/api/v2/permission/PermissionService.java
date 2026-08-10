package buildcraft.api.v2.permission;

/**
 * Protection hook for world operations. Implementations must be observational:
 * permission checks, including SIMULATE, must never mutate the world.
 */
@FunctionalInterface
public interface PermissionService {
    PermissionDecision decide(WorldOperationContext context);

    static PermissionService passThrough() {
        return context -> PermissionDecision.pass();
    }
}
