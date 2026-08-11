package buildcraft.api.v2.pipe;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.Direction;

/** Weighted route candidates. Weight 0 means blocked. */
public final class RouteDecision {
    private final Map<Direction, Integer> weights;

    public RouteDecision(Map<Direction, Integer> weights) {
        Objects.requireNonNull(weights, "weights");
        EnumMap<Direction, Integer> copy = new EnumMap<>(Direction.class);
        for (Map.Entry<Direction, Integer> entry : weights.entrySet()) {
            int weight = Objects.requireNonNull(entry.getValue(), "weight");
            if (weight < 0) throw new IllegalArgumentException("route weight must be non-negative");
            copy.put(Objects.requireNonNull(entry.getKey(), "direction"), weight);
        }
        this.weights = Collections.unmodifiableMap(copy);
    }
    public Map<Direction, Integer> weights() { return weights; }
    public boolean blocked() { return weights.values().stream().noneMatch(weight -> weight > 0); }
}
