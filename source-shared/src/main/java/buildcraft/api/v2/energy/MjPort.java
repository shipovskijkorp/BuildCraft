package buildcraft.api.v2.energy;

import buildcraft.api.v2.OperationMode;
import java.util.Objects;

/** Loader-neutral MJ transfer endpoint. */
public interface MjPort {
    MjTransferResult insert(MjAmount offered, OperationMode mode);
    MjTransferResult extract(MjAmount requested, OperationMode mode);
    MjAmount stored();
    MjAmount capacity();

    default boolean canInsert() { return true; }
    default boolean canExtract() { return true; }

    default MjTransferResult insert(MjAmount offered, MjTransferPolicy policy, OperationMode mode) {
        Objects.requireNonNull(offered, "offered");
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(mode, "mode");
        if (policy == MjTransferPolicy.PARTIAL) return insert(offered, mode);
        MjTransferResult simulated = insert(offered, OperationMode.SIMULATE);
        if (!simulated.completed()) return MjTransferResult.none(offered);
        return mode == OperationMode.SIMULATE ? simulated : insert(offered, OperationMode.EXECUTE);
    }

    default MjTransferResult extract(MjAmount minimum, MjAmount maximum, OperationMode mode) {
        Objects.requireNonNull(minimum, "minimum");
        Objects.requireNonNull(maximum, "maximum");
        Objects.requireNonNull(mode, "mode");
        if (minimum.compareTo(maximum) > 0) throw new IllegalArgumentException("minimum must be <= maximum");
        MjTransferResult simulated = extract(maximum, OperationMode.SIMULATE);
        if (simulated.transferred().compareTo(minimum) < 0) return MjTransferResult.none(maximum);
        return mode == OperationMode.SIMULATE ? simulated : extract(maximum, OperationMode.EXECUTE);
    }
}
