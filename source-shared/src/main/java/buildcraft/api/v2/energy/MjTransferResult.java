package buildcraft.api.v2.energy;

import java.util.Objects;

/** Conservation-friendly result: requested = transferred + remainder. */
public record MjTransferResult(MjAmount requested, MjAmount transferred, MjAmount remainder) {
    public MjTransferResult {
        Objects.requireNonNull(requested, "requested");
        Objects.requireNonNull(transferred, "transferred");
        Objects.requireNonNull(remainder, "remainder");
        if (Math.addExact(transferred.microMj(), remainder.microMj()) != requested.microMj()) {
            throw new IllegalArgumentException("requested must equal transferred + remainder");
        }
    }
    public static MjTransferResult none(MjAmount requested) { return new MjTransferResult(requested, MjAmount.ZERO, requested); }
    public static MjTransferResult of(MjAmount requested, MjAmount transferred) {
        return new MjTransferResult(requested, transferred, MjAmount.ofMicro(requested.microMj() - transferred.microMj()));
    }
    public boolean completed() { return remainder.isZero(); }
}
