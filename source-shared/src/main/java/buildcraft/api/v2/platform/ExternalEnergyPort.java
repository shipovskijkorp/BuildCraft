package buildcraft.api.v2.platform;

import buildcraft.api.v2.OperationMode;

/** Loader-neutral port for an external integer energy system such as Forge Energy. */
public interface ExternalEnergyPort {
    long insert(long offered, OperationMode mode);
    long extract(long requested, OperationMode mode);
    long stored();
    long capacity();

    default boolean canInsert() { return true; }
    default boolean canExtract() { return true; }
}
