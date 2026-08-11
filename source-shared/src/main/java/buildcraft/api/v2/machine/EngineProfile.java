package buildcraft.api.v2.machine;

import buildcraft.api.v2.energy.MjAmount;
import java.util.Objects;

public record EngineProfile(MjAmount maxOutputPerTick, MjAmount capacity, boolean acceptsExternalEnergy) {
    public EngineProfile {
        Objects.requireNonNull(maxOutputPerTick, "maxOutputPerTick"); Objects.requireNonNull(capacity, "capacity");
    }
}
