package buildcraft.api.v2.fluid;

import buildcraft.api.v2.OperationMode;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FluidPortTester {
    @Test
    public void simulateDoesNotMutateAndExecuteMayPartiallyAccept() {
        FluidVariant water = FluidVariant.of(id("water"));
        FakeTank tank = new FakeTank(1000, variant -> true);

        FluidTransferResult simulated = tank.insert(FluidVolume.of(water, 1500), OperationMode.SIMULATE);
        assertEquals(1000, simulated.transferredAmount().milliBuckets());
        assertEquals(500, simulated.remainderAmount().milliBuckets());
        assertEquals(0, tank.amount);

        FluidTransferResult executed = tank.insert(FluidVolume.of(water, 1500), OperationMode.EXECUTE);
        assertEquals(1000, executed.transferredAmount().milliBuckets());
        assertEquals(1000, tank.amount);
    }

    @Test
    public void exactVariantAndSameFluidAreDifferentMatchers() {
        FluidVariant plain = FluidVariant.of(id("fuel"));
        FluidVariant special = FluidVariant.of(
            id("fuel"),
            FluidComponentPayload.of(id("components"), new byte[] {42})
        );
        FluidMatchContext context = (fluid, tag) -> false;

        assertFalse(FluidMatcher.exact(plain).matches(special, context));
        assertTrue(FluidMatcher.fluid(id("fuel")).matches(special, context));
    }

    @Test
    public void tagMatcherUsesContextAndExtractionRespectsSimulation() {
        ResourceLocation coldTag = id("cold");
        FluidVariant water = FluidVariant.of(id("water"));
        FakeTank tank = new FakeTank(1000, variant -> true);
        tank.variant = water;
        tank.amount = 750;
        tank.tags.add(coldTag);

        FluidTransferResult simulated = tank.extract(FluidMatcher.tag(coldTag), FluidAmount.of(500), OperationMode.SIMULATE);
        assertEquals(500, simulated.transferredAmount().milliBuckets());
        assertEquals(750, tank.amount);

        FluidTransferResult executed = tank.extract(FluidMatcher.tag(coldTag), FluidAmount.of(500), OperationMode.EXECUTE);
        assertEquals(500, executed.transferredAmount().milliBuckets());
        assertEquals(250, tank.amount);
    }

    private static final class FakeTank implements FluidPort {
        private final long capacity;
        private final java.util.function.Predicate<FluidVariant> allowInsert;
        private final Set<ResourceLocation> tags = new HashSet<>();
        private FluidVariant variant;
        private long amount;

        FakeTank(long capacity, java.util.function.Predicate<FluidVariant> allowInsert) {
            this.capacity = capacity;
            this.allowInsert = allowInsert;
        }

        @Override
        public FluidTransferResult insert(FluidVolume offered, OperationMode mode) {
            if (offered.isEmpty()) {
                return FluidTransferResult.ofInsertion(offered, FluidAmount.ZERO);
            }
            FluidVariant offeredVariant = offered.requireVariant();
            if (!allowInsert.test(offeredVariant)) {
                return FluidTransferResult.ofInsertion(offered, FluidAmount.ZERO);
            }
            if (amount > 0 && !variant.equals(offeredVariant)) {
                return FluidTransferResult.ofInsertion(offered, FluidAmount.ZERO);
            }
            long accepted = Math.min(offered.amount().milliBuckets(), capacity - amount);
            if (mode == OperationMode.EXECUTE && accepted > 0) {
                if (amount == 0) {
                    variant = offeredVariant;
                }
                amount += accepted;
            }
            return FluidTransferResult.ofInsertion(offered, FluidAmount.of(accepted));
        }

        @Override
        public FluidTransferResult extract(FluidMatcher matcher, FluidAmount maxAmount, OperationMode mode) {
            if (amount == 0 || variant == null || !matcher.matches(variant, this::isInTag)) {
                return FluidTransferResult.nothing(maxAmount);
            }
            long extracted = Math.min(amount, maxAmount.milliBuckets());
            FluidVolume moved = FluidVolume.of(variant, extracted);
            if (mode == OperationMode.EXECUTE) {
                amount -= extracted;
                if (amount == 0) {
                    variant = null;
                }
            }
            return FluidTransferResult.ofExtraction(maxAmount, moved);
        }

        private boolean isInTag(ResourceLocation fluidId, ResourceLocation tagId) {
            return variant != null && variant.fluidId().equals(fluidId) && tags.contains(tagId);
        }
    }

    private static ResourceLocation id(String path) {
        return Objects.requireNonNull(ResourceLocation.tryParse("test:" + path));
    }
}
