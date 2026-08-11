package buildcraft.api.v2.machine;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record EngineStage(ResourceLocation id, int severity) {
    public EngineStage {
        Objects.requireNonNull(id, "id");
        if (severity < 0) throw new IllegalArgumentException("severity must be non-negative");
    }
}
