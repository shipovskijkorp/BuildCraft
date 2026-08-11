package buildcraft.api.v2.energy;

import buildcraft.api.v2.OperationMode;

/** Loader-neutral MJ transfer endpoint. */
public interface MjPort {
    MjTransferResult insert(MjAmount offered, OperationMode mode);
    MjTransferResult extract(MjAmount requested, OperationMode mode);
    MjAmount stored();
    MjAmount capacity();

    default boolean canInsert() { return true; }
    default boolean canExtract() { return true; }
}
