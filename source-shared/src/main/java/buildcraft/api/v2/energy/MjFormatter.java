package buildcraft.api.v2.energy;

/** Player-facing MJ formatting using the active BuildCraft unit preferences. */
public interface MjFormatter {
    /** Numeric MJ amount without a unit suffix, useful when embedding into translated text. */
    String formatNumber(MjAmount amount);
    String formatAmount(MjAmount amount);
    String formatRate(MjAmount perTick, EnergyRateUnit unit);
}
