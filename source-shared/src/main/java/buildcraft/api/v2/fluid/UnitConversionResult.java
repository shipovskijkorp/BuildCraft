package buildcraft.api.v2.fluid;

/**
 * Exact rational conversion result.
 *
 * The represented value is {@code whole + remainder / divisor}. The remainder
 * is always in [0, divisor), allowing platform bridges to reject inexact
 * conversions instead of silently rounding.
 */
public final class UnitConversionResult {
    private final long whole;
    private final long remainder;
    private final long divisor;

    UnitConversionResult(long whole, long remainder, long divisor) {
        this.whole = whole;
        this.remainder = remainder;
        this.divisor = divisor;
    }

    public long whole() {
        return whole;
    }

    public long remainder() {
        return remainder;
    }

    public long divisor() {
        return divisor;
    }

    public boolean exact() {
        return remainder == 0;
    }
}
