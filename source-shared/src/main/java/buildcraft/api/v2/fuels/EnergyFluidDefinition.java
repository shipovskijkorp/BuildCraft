package buildcraft.api.v2.fuels;

/** Marker for the atomically published fuels/coolants definition set. */
public interface EnergyFluidDefinition {
    Kind kind();

    enum Kind {
        FUEL,
        COOLANT,
        SOLID_COOLANT
    }
}
