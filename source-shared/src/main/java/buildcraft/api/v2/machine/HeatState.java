package buildcraft.api.v2.machine;

public record HeatState(double temperature, double criticalTemperature) {
    public HeatState {
        if (!Double.isFinite(temperature) || !Double.isFinite(criticalTemperature) || criticalTemperature < 0.0) {
            throw new IllegalArgumentException("temperatures must be finite and criticalTemperature non-negative");
        }
    }
    public boolean critical() { return temperature >= criticalTemperature; }
}
