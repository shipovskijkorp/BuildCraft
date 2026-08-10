package buildcraft.api.v2.fuels;

/** Data representation for a constant-rate coolant. */
public record CoolantProfileData(FluidSelectorData selector, double degreesPerMilliBucket) {}
