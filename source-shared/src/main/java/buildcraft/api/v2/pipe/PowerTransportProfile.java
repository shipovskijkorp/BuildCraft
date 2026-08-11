package buildcraft.api.v2.pipe;

import buildcraft.api.v2.energy.MjAmount;
import java.util.Objects;

/** Validated MJ pipe transport limits. Resistance is expressed in micro-MJ per MJ (0..1,000,000). */
public record PowerTransportProfile(MjAmount maxPerTick, MjAmount lossAtFullTransfer, long resistancePerTick, boolean extractor) {
    public PowerTransportProfile {
        Objects.requireNonNull(maxPerTick, "maxPerTick");
        Objects.requireNonNull(lossAtFullTransfer, "lossAtFullTransfer");
        if (maxPerTick.isZero()) throw new IllegalArgumentException("maxPerTick must be positive");
        if (lossAtFullTransfer.compareTo(maxPerTick) > 0) throw new IllegalArgumentException("loss cannot exceed maxPerTick");
        if (resistancePerTick < 0 || resistancePerTick > MjAmount.MICRO_MJ_PER_MJ) {
            throw new IllegalArgumentException("resistancePerTick must be within [0, 1 MJ]");
        }
    }

    public static PowerTransportProfile fromLoss(MjAmount maxPerTick, MjAmount lossAtFullTransfer, boolean extractor) {
        Objects.requireNonNull(maxPerTick, "maxPerTick");
        Objects.requireNonNull(lossAtFullTransfer, "lossAtFullTransfer");
        if (maxPerTick.isZero()) throw new IllegalArgumentException("maxPerTick must be positive");
        long resistance = Math.multiplyExact(lossAtFullTransfer.microMj(), MjAmount.MICRO_MJ_PER_MJ) / maxPerTick.microMj();
        return new PowerTransportProfile(maxPerTick, lossAtFullTransfer, resistance, extractor);
    }

    public static PowerTransportProfile fromResistance(MjAmount maxPerTick, long resistancePerTick, boolean extractor) {
        Objects.requireNonNull(maxPerTick, "maxPerTick");
        if (resistancePerTick < 0 || resistancePerTick > MjAmount.MICRO_MJ_PER_MJ) {
            throw new IllegalArgumentException("resistancePerTick must be within [0, 1 MJ]");
        }
        long loss = Math.multiplyExact(resistancePerTick, maxPerTick.microMj()) / MjAmount.MICRO_MJ_PER_MJ;
        return new PowerTransportProfile(maxPerTick, MjAmount.ofMicro(loss), resistancePerTick, extractor);
    }
}
