package buildcraft.api.v2.drop;

import buildcraft.api.v2.fluid.FluidVolume;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.world.level.Level;

/** Context for converting stored fluid into item drops. The world is optional for inventory-only callers. */
public record FluidDropContext(Optional<Level> level, FluidVolume fluid) {
    public FluidDropContext {
        level = Objects.requireNonNull(level, "level");
        fluid = Objects.requireNonNull(fluid, "fluid");
    }

    public static FluidDropContext of(FluidVolume fluid) {
        return new FluidDropContext(Optional.empty(), fluid);
    }

    public static FluidDropContext of(Level level, FluidVolume fluid) {
        return new FluidDropContext(Optional.of(Objects.requireNonNull(level, "level")), fluid);
    }
}
