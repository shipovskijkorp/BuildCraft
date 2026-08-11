package buildcraft.api.v2.energy;

/** Runtime view of BuildCraft's MJ/Forge Energy policy. */
public interface EnergyService {
    EnergyConversion conversion();
    boolean automaticFeConversionEnabled();
    boolean displayForgeEnergy();
}
