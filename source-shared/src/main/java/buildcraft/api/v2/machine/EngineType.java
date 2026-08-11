package buildcraft.api.v2.machine;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record EngineType(ResourceLocation id, EngineProfile profile) {
    public EngineType {
        Objects.requireNonNull(id, "id"); Objects.requireNonNull(profile, "profile");
    }
}
