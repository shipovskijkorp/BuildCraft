package buildcraft.api.v2.pipe;

import buildcraft.api.v2.fluid.FluidAmount;
import java.util.Objects;

public record FluidTransportProfile(FluidAmount maxPerTick, int transferDelayTicks) {
    public FluidTransportProfile {
        Objects.requireNonNull(maxPerTick, "maxPerTick");
        if (transferDelayTicks < 0) throw new IllegalArgumentException("transferDelayTicks must be non-negative");
    }
}
