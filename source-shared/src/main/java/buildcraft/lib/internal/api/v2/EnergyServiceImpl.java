package buildcraft.lib.internal.api.v2;

import buildcraft.api.v2.energy.EnergyConversion;
import buildcraft.api.v2.energy.EnergyConversionStatus;
import buildcraft.api.v2.energy.EnergyRateUnit;
import buildcraft.api.v2.energy.EnergyService;
import buildcraft.api.v2.energy.MjAmount;
import buildcraft.api.v2.energy.MjStorage;
import buildcraft.lib.internal.api.v2.energy.MjStorageImpl;
import buildcraft.lib.BCLibConfig;

final class EnergyServiceImpl implements EnergyService {
    @Override
    public EnergyConversion conversion() {
        return new EnergyConversion(BCLibConfig.mjFeConversion.mjPerFe);
    }

    @Override public boolean automaticFeConversionEnabled() { return BCLibConfig.powerMode.isAutoconvertEnabled(); }
    @Override public boolean displayForgeEnergy() { return BCLibConfig.powerMode.isDisplayFe(); }

    @Override
    public EnergyConversionStatus status() {
        EnergyRateUnit rateUnit = BCLibConfig.displayTimeGap == BCLibConfig.TimeGap.TICKS
            ? EnergyRateUnit.PER_TICK
            : EnergyRateUnit.PER_SECOND;
        return new EnergyConversionStatus(conversion(), automaticFeConversionEnabled(), displayForgeEnergy(), rateUnit);
    }
    @Override
    public MjStorage createStorage(MjAmount capacity, MjAmount initial) {
        return new MjStorageImpl(capacity, initial);
    }

}
