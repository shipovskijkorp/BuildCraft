package buildcraft.api.v2.robot;

import buildcraft.api.v2.persistence.OpaqueData;
import buildcraft.api.v2.persistence.PersistentType;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record RobotTaskType<T extends RobotTask>(ResourceLocation id, PersistentType<T, OpaqueData> persistence) {
    public RobotTaskType {
        Objects.requireNonNull(id, "id"); Objects.requireNonNull(persistence, "persistence");
    }
}
