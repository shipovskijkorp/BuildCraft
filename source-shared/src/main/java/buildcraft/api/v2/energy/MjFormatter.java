package buildcraft.api.v2.energy;

public interface MjFormatter {
    String formatAmount(MjAmount amount);
    String formatRate(MjAmount perTick, EnergyRateUnit unit);
}
