package buildcraft.api.v2.pipe;

import java.util.Objects;
import java.util.Set;
import net.minecraft.core.Direction;

public record ExternalEnergyRouteContext(PipeView pipe, Direction input, long amount, Set<Direction> candidates) {
    public ExternalEnergyRouteContext {
        Objects.requireNonNull(pipe, "pipe"); Objects.requireNonNull(input, "input");
        if (amount < 0) throw new IllegalArgumentException("amount must be non-negative");
        candidates = Set.copyOf(Objects.requireNonNull(candidates, "candidates"));
    }
}
