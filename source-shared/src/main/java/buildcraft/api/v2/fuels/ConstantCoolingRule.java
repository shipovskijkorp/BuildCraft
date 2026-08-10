package buildcraft.api.v2.fuels;

import buildcraft.api.v2.fluid.FluidVariant;

public record ConstantCoolingRule(double degreesPerMilliBucket) implements CoolingRule {
    public ConstantCoolingRule {
        if (!Double.isFinite(degreesPerMilliBucket) || degreesPerMilliBucket <= 0) {
            throw new IllegalArgumentException("degreesPerMilliBucket must be finite and > 0");
        }
    }

    @Override
    public double degreesPerMilliBucket(FluidVariant fluid, double heat) {
        return degreesPerMilliBucket;
    }
}
