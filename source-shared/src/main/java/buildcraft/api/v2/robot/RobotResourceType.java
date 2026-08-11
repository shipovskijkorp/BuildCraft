package buildcraft.api.v2.robot;

import buildcraft.api.v2.persistence.OpaqueData;
import buildcraft.api.v2.persistence.PersistentType;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record RobotResourceType<R extends RobotResource>(ResourceLocation id, PersistentType<R, OpaqueData> persistence) {
    public RobotResourceType {
        Objects.requireNonNull(id, "id"); Objects.requireNonNull(persistence, "persistence");
    }
}
