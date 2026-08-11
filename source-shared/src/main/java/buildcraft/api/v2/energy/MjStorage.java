package buildcraft.api.v2.energy;

/**
 * Stable mutable MJ storage contract. Runtime implementations live in BuildCraft Lib.
 * Addons should obtain a standard implementation through {@link EnergyService#createStorage}.
 */
public interface MjStorage extends MjPort {
    MjAmount capacity();
    MjAmount stored();
}
