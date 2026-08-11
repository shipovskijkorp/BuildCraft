package buildcraft.api.v2.machine;

/** Stable operating range for a heat-bearing machine, measured in BuildCraft degrees. */
public record HeatProfile(double minimum, double ideal, double maximum) {
    public HeatProfile {
        if (!Double.isFinite(minimum) || !Double.isFinite(ideal) || !Double.isFinite(maximum)) {
            throw new IllegalArgumentException("heat values must be finite");
        }
        if (ideal < minimum || maximum < ideal) {
            throw new IllegalArgumentException("heat profile must satisfy minimum <= ideal <= maximum");
        }
    }
}
