package buildcraft.api.v2.fluid;

import buildcraft.api.v2.OperationMode;

/**
 * Loader-neutral insertion/extraction endpoint.
 *
 * Implementations MUST NOT mutate state in {@link OperationMode#SIMULATE}.
 * Partial transfers are normal and are reported explicitly in the result.
 */
public interface FluidPort {
    FluidTransferResult insert(FluidVolume offered, OperationMode mode);

    FluidTransferResult extract(FluidMatcher matcher, FluidAmount maxAmount, OperationMode mode);
}
