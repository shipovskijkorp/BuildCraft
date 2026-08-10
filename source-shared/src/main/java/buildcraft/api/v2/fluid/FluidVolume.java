package buildcraft.api.v2.fluid;

import java.util.Objects;
import java.util.Optional;

/** Immutable fluid value: variant plus non-negative amount. */
public final class FluidVolume {
    private static final FluidVolume EMPTY = new FluidVolume(null, FluidAmount.ZERO);

    private final FluidVariant variant;
    private final FluidAmount amount;

    private FluidVolume(FluidVariant variant, FluidAmount amount) {
        this.variant = variant;
        this.amount = amount;
    }

    public static FluidVolume empty() {
        return EMPTY;
    }

    public static FluidVolume of(FluidVariant variant, long milliBuckets) {
        return of(variant, FluidAmount.of(milliBuckets));
    }

    public static FluidVolume of(FluidVariant variant, FluidAmount amount) {
        Objects.requireNonNull(variant, "variant");
        Objects.requireNonNull(amount, "amount");
        if (amount.isZero()) {
            return EMPTY;
        }
        return new FluidVolume(variant, amount);
    }

    public boolean isEmpty() {
        return amount.isZero();
    }

    public Optional<FluidVariant> variant() {
        return Optional.ofNullable(variant);
    }

    public FluidVariant requireVariant() {
        if (variant == null) {
            throw new IllegalStateException("Empty fluid volume has no variant");
        }
        return variant;
    }

    public FluidAmount amount() {
        return amount;
    }

    public FluidVolume withAmount(FluidAmount newAmount) {
        Objects.requireNonNull(newAmount, "newAmount");
        if (newAmount.isZero()) {
            return EMPTY;
        }
        if (variant == null) {
            throw new IllegalStateException("Cannot assign a non-zero amount to an empty volume without a variant");
        }
        return of(variant, newAmount);
    }

    public boolean sameVariant(FluidVolume other) {
        return !isEmpty() && other != null && !other.isEmpty() && variant.equals(other.variant);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this
            || obj instanceof FluidVolume other
            && Objects.equals(variant, other.variant)
            && amount.equals(other.amount);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hashCode(variant) + amount.hashCode();
    }

    @Override
    public String toString() {
        return isEmpty() ? "FluidVolume[empty]" : "FluidVolume[" + variant + ", " + amount + "]";
    }
}
