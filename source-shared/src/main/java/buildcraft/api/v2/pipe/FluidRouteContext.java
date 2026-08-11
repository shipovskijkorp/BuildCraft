package buildcraft.api.v2.pipe;

import buildcraft.api.v2.fluid.FluidVolume;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.Direction;

public record FluidRouteContext(PipeView pipe, Direction input, FluidVolume volume, Set<Direction> candidates) {
    public FluidRouteContext {
        Objects.requireNonNull(pipe, "pipe"); Objects.requireNonNull(input, "input"); Objects.requireNonNull(volume, "volume");
        candidates = Set.copyOf(Objects.requireNonNull(candidates, "candidates"));
    }
}
