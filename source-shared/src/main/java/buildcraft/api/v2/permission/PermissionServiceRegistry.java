package buildcraft.api.v2.permission;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * Runtime permission service and addon registration point.
 *
 * Providers are evaluated by descending priority and then stable provider id.
 * DENY is terminal, while ALLOW is retained and lower-priority providers are
 * still allowed to deny the operation. Duplicate provider ids are rejected.
 */
public interface PermissionServiceRegistry extends PermissionService {
    void register(ResourceLocation id, int priority, PermissionService service);

    List<PermissionProvider> providers();
}
