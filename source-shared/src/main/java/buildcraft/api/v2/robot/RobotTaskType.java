package buildcraft.api.v2.robot;

import buildcraft.api.v2.persistence.OpaqueData;
import buildcraft.api.v2.persistence.PersistentType;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Persisted robot task type. Robot controls accept only task instances backed by a registered type. */
public record RobotTaskType<T extends RobotTask>(
    ResourceLocation id,
    Class<T> taskType,
    PersistentType<T, OpaqueData> persistence
) {
    public RobotTaskType {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(taskType, "taskType");
        Objects.requireNonNull(persistence, "persistence");
        if (!id.equals(persistence.id())) {
            throw new IllegalArgumentException("Robot task registry id must match persistent type id: " + id + " != " + persistence.id());
        }
    }
}
