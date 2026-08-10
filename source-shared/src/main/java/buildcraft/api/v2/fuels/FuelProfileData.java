package buildcraft.api.v2.fuels;

import buildcraft.api.v2.fluid.FluidVariantData;

/** Data representation for the stable built-in FuelProfile form. */
public record FuelProfileData(
    FluidSelectorData selector,
    long powerPerTickMicroMj,
    int burnTicksPerBucket,
    FluidVariantData residueVariant,
    long residueMilliBuckets
) {}
