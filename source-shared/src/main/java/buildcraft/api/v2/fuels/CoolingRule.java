package buildcraft.api.v2.fuels;

import buildcraft.api.v2.fluid.FluidVariant;

/** Programmatic cooling curve. Data-driven profiles use ConstantCoolingRule. */
@FunctionalInterface
public interface CoolingRule {
    double degreesPerMilliBucket(FluidVariant fluid, double heat);
}
