package buildcraft.lib.api.v2;

import buildcraft.api.v2.energy.EnergyConversion;
import buildcraft.api.v2.energy.EnergyService;
import buildcraft.lib.BCLibConfig;

final class EnergyServiceImpl implements EnergyService {
    @Override
    public EnergyConversion conversion() {
        return new EnergyConversion(BCLibConfig.mjFeConversion.mjPerFe);
    }

    @Override public boolean automaticFeConversionEnabled() { return BCLibConfig.powerMode.isAutoconvertEnabled(); }
    @Override public boolean displayForgeEnergy() { return BCLibConfig.powerMode.isDisplayFe(); }
}
