package buildcraft.api.v2.robot;

import buildcraft.api.v2.persistence.OpaqueData;
import buildcraft.api.v2.persistence.PersistentType;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Persisted robot resource type plus the runtime acquisition strategy that makes the extension live. */
public record RobotResourceType<R extends RobotResource>(
    ResourceLocation id,
    Class<R> resourceType,
    PersistentType<R, OpaqueData> persistence,
    RobotResourceAcquirer<R> acquirer
) {
    public RobotResourceType {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(resourceType, "resourceType");
        Objects.requireNonNull(persistence, "persistence");
        Objects.requireNonNull(acquirer, "acquirer");
        if (!id.equals(persistence.id())) {
            throw new IllegalArgumentException("Robot resource registry id must match persistent type id: " + id + " != " + persistence.id());
        }
    }
}
