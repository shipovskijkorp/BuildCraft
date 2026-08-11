package buildcraft.api.v2.filler;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Registered filler pattern descriptor independent from statement implementation. */
public record FillerPatternType(ResourceLocation id, FillerPattern pattern, boolean internalOnly) {
    public FillerPatternType {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(pattern, "pattern");
    }
}
