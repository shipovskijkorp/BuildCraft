package buildcraft.api.v2.energy;

import java.util.Objects;

/** Snapshot of the active MJ/external-energy compatibility policy. */
public record EnergyConversionStatus(
    EnergyConversion conversion,
    boolean automaticConversionEnabled,
    boolean displayExternalEnergy,
    EnergyRateUnit displayRateUnit
) {
    public EnergyConversionStatus {
        Objects.requireNonNull(conversion, "conversion");
        Objects.requireNonNull(displayRateUnit, "displayRateUnit");
    }
}
