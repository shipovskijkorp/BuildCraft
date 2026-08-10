package buildcraft.api.v2.fluid;

import java.math.BigInteger;
import java.util.Objects;

/** Exact conversion helper between mB and a platform's units-per-bucket scale. */
public final class FluidUnitConverter {
    private static final BigInteger THOUSAND = BigInteger.valueOf(FluidAmount.MILLIBUCKETS_PER_BUCKET);

    private FluidUnitConverter() {
    }

    public static UnitConversionResult toPlatformUnits(FluidAmount amount, long unitsPerBucket) {
        Objects.requireNonNull(amount, "amount");
        if (unitsPerBucket <= 0) {
            throw new IllegalArgumentException("unitsPerBucket must be positive");
        }
        BigInteger numerator = BigInteger.valueOf(amount.milliBuckets()).multiply(BigInteger.valueOf(unitsPerBucket));
        return divide(numerator, THOUSAND);
    }

    public static UnitConversionResult fromPlatformUnits(long platformUnits, long unitsPerBucket) {
        if (platformUnits < 0) {
            throw new IllegalArgumentException("platformUnits must be non-negative");
        }
        if (unitsPerBucket <= 0) {
            throw new IllegalArgumentException("unitsPerBucket must be positive");
        }
        BigInteger numerator = BigInteger.valueOf(platformUnits).multiply(THOUSAND);
        return divide(numerator, BigInteger.valueOf(unitsPerBucket));
    }

    private static UnitConversionResult divide(BigInteger numerator, BigInteger divisor) {
        BigInteger[] result = numerator.divideAndRemainder(divisor);
        if (result[0].bitLength() > 63) {
            throw new ArithmeticException("Converted fluid amount exceeds signed 64-bit range");
        }
        return new UnitConversionResult(result[0].longValueExact(), result[1].longValueExact(), divisor.longValueExact());
    }
}
