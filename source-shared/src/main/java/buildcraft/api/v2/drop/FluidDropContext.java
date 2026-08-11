package buildcraft.api.v2.drop;

import buildcraft.api.v2.fluid.FluidVolume;
import java.util.Objects;
import net.minecraft.world.level.Level;

public record FluidDropContext(Level level, FluidVolume fluid) {
    public FluidDropContext {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(fluid, "fluid");
    }
}
