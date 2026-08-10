package buildcraft.api.v2.permission;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** One named permission integration with deterministic evaluation priority. */
public record PermissionProvider(ResourceLocation id, int priority, PermissionService service) {
    public PermissionProvider {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(service, "service");
    }
}
