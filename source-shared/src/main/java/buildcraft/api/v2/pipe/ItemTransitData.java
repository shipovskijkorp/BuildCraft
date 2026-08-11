package buildcraft.api.v2.pipe;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.world.item.DyeColor;

/** Routing metadata carried by an item travelling through a BuildCraft pipe. */
public record ItemTransitData(Optional<DyeColor> color, double speedBlocksPerTick) {
    public static final ItemTransitData DEFAULT = new ItemTransitData(Optional.empty(), 0.0);

    public ItemTransitData {
        color = Objects.requireNonNull(color, "color");
        if (!Double.isFinite(speedBlocksPerTick) || speedBlocksPerTick < 0.0) {
            throw new IllegalArgumentException("speedBlocksPerTick must be finite and non-negative");
        }
    }
}
