package buildcraft.api.v2.filler;

import buildcraft.api.v2.area.BlockBox;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public record FillerPatternContext(BlockBox bounds, Direction facing, Map<ResourceLocation, String> options) {
    public FillerPatternContext {
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(facing, "facing");
        options = Map.copyOf(Objects.requireNonNull(options, "options"));
    }
}
