package buildcraft.api.v2.fluid;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FluidModelTester {
    @Test
    public void variantIdentityIncludesOpaqueComponentsAndDefensivelyCopiesBytes() {
        byte[] source = new byte[] {1, 2, 3};
        FluidVariant plain = FluidVariant.of(id("water"));
        FluidVariant component = FluidVariant.of(id("water"), FluidComponentPayload.of(id("component_format"), source));
        FluidVariant sameComponent = FluidVariant.of(
            id("water"),
            FluidComponentPayload.of(id("component_format"), new byte[] {1, 2, 3})
        );

        source[0] = 99;

        assertTrue(plain.sameFluid(component));
        assertNotEquals(plain, component);
        assertEquals(component, sameComponent);
        assertEquals(component.stableHash64(), sameComponent.stableHash64());
        assertArrayEquals(new byte[] {1, 2, 3}, component.components().copyCanonicalBytes());
    }

    @Test
    public void variantCodecRoundTrips() {
        FluidVariant original = FluidVariant.of(
            id("fuel"),
            FluidComponentPayload.of(id("canonical"), new byte[] {10, 20})
        );
        FluidVariantData encoded = FluidVariant.CODEC.encode(original).valueOrThrow();
        FluidVariant decoded = FluidVariant.CODEC.decode(encoded).valueOrThrow();
        assertEquals(original, decoded);
    }

    @Test
    public void amountRejectsNegativeAndOverflow() {
        assertThrows(IllegalArgumentException.class, () -> FluidAmount.of(-1));
        assertThrows(ArithmeticException.class, () -> FluidAmount.of(Long.MAX_VALUE).plus(FluidAmount.of(1)));
        assertThrows(IllegalArgumentException.class, () -> FluidAmount.of(1).minus(FluidAmount.of(2)));
    }

    @Test
    public void zeroVolumeDoesNotRetainVariantIdentity() {
        assertTrue(FluidVolume.of(FluidVariant.of(id("water")), 0).isEmpty());
        assertFalse(FluidVolume.empty().variant().isPresent());
        assertThrows(IllegalStateException.class, FluidVolume.empty()::requireVariant);
    }

    @Test
    public void unitConversionReportsRemainderInsteadOfRounding() {
        UnitConversionResult exact = FluidUnitConverter.toPlatformUnits(FluidAmount.of(250), 81_000);
        assertTrue(exact.exact());
        assertEquals(20_250, exact.whole());

        UnitConversionResult inexact = FluidUnitConverter.toPlatformUnits(FluidAmount.of(1), 3);
        assertFalse(inexact.exact());
        assertEquals(0, inexact.whole());
        assertEquals(3, inexact.remainder());
        assertEquals(1000, inexact.divisor());

        UnitConversionResult reverse = FluidUnitConverter.fromPlatformUnits(20_250, 81_000);
        assertTrue(reverse.exact());
        assertEquals(250, reverse.whole());
    }

    private static ResourceLocation id(String path) {
        return Objects.requireNonNull(ResourceLocation.tryParse("test:" + path));
    }
}
