package buildcraft.api.v2.energy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EnergyConversionTester {
    @Test
    public void defaultBuildCraftRatioIsConservative() {
        EnergyConversion conversion = new EnergyConversion(100_000);
        assertEquals(4_000_000L, conversion.feToMicroMj(40));
        assertEquals(40L, conversion.microMjToWholeFe(4_000_000L));
        assertEquals(0L, conversion.conversionRemainder(4_000_000L));
    }

    @Test
    public void fractionalMjIsReportedAsRemainderRatherThanRoundedUp() {
        EnergyConversion conversion = new EnergyConversion(100_000);
        assertEquals(4L, conversion.microMjToWholeFe(499_999));
        assertEquals(99_999L, conversion.conversionRemainder(499_999));
    }

    @Test
    public void mjAmountsAndTransferResultsRejectCreationOfEnergy() {
        assertThrows(IllegalArgumentException.class, () -> new MjAmount(-1));
        assertThrows(ArithmeticException.class, () -> MjAmount.ofMicro(Long.MAX_VALUE).plus(MjAmount.ofMicro(1)));
        assertThrows(IllegalArgumentException.class, () -> new MjTransferResult(
            MjAmount.ofMicro(10), MjAmount.ofMicro(8), MjAmount.ofMicro(3)
        ));

        MjTransferResult result = MjTransferResult.of(MjAmount.ofMj(4), MjAmount.ofMj(3));
        assertEquals(MjAmount.ofMj(1), result.remainder());
        assertTrue(!result.completed());
    }
}
