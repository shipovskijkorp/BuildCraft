package buildcraft.api.v2.recipe;

import buildcraft.api.v2.fluid.FluidAmount;
import buildcraft.api.v2.fluid.FluidMatchContext;
import buildcraft.api.v2.fluid.FluidMatcher;
import buildcraft.api.v2.fluid.FluidVariant;
import buildcraft.api.v2.persistence.ApiCodec;
import buildcraft.api.v2.persistence.CodecResult;
import java.util.Objects;
import java.util.Optional;

/** Fluid matcher plus the amount consumed by a machine recipe. */
public final class FluidIngredient {
    public static final ApiCodec<FluidIngredient, FluidIngredientData> CODEC = new ApiCodec<>() {
        @Override
        public CodecResult<FluidIngredient> decode(FluidIngredientData data) {
            if (data == null || data.variant() == null) return CodecResult.failure("Fluid ingredient payload is missing variant");
            try {
                FluidVariant variant = FluidVariant.CODEC.decode(data.variant()).valueOrThrow();
                return CodecResult.success(exact(variant, data.milliBuckets()));
            } catch (RuntimeException ex) {
                return CodecResult.failure(safeMessage(ex));
            }
        }

        @Override
        public CodecResult<FluidIngredientData> encode(FluidIngredient value) {
            if (value == null) return CodecResult.failure("Fluid ingredient is null");
            if (value.representative == null) return CodecResult.failure("Programmatic fluid matcher is not data-serializable");
            CodecResult<buildcraft.api.v2.fluid.FluidVariantData> encoded = FluidVariant.CODEC.encode(value.representative);
            return encoded.successful()
                ? CodecResult.success(new FluidIngredientData(encoded.valueOrThrow(), value.amount.milliBuckets()))
                : CodecResult.failure(String.join("; ", encoded.errors()));
        }

        private String safeMessage(RuntimeException ex) {
            String msg = ex.getMessage();
            return msg == null || msg.isBlank() ? ex.getClass().getSimpleName() : msg;
        }
    };

    private final FluidMatcher matcher;
    private final FluidAmount amount;
    private final FluidVariant representative;

    private FluidIngredient(FluidMatcher matcher, FluidAmount amount, FluidVariant representative) {
        this.matcher = Objects.requireNonNull(matcher, "matcher");
        this.amount = Objects.requireNonNull(amount, "amount");
        if (amount.isZero()) throw new IllegalArgumentException("Recipe fluid amount must be > 0");
        this.representative = representative;
    }

    public static FluidIngredient exact(FluidVariant variant, long milliBuckets) {
        Objects.requireNonNull(variant, "variant");
        return new FluidIngredient(FluidMatcher.exact(variant), FluidAmount.of(milliBuckets), variant);
    }

    public static FluidIngredient matching(FluidMatcher matcher, FluidAmount amount) {
        return new FluidIngredient(matcher, amount, null);
    }

    public FluidMatcher matcher() { return matcher; }
    public FluidAmount amount() { return amount; }
    public Optional<FluidVariant> representativeVariant() { return Optional.ofNullable(representative); }

    public boolean matches(FluidVariant fluid, FluidMatchContext context) {
        return matcher.matches(Objects.requireNonNull(fluid, "fluid"), Objects.requireNonNull(context, "context"));
    }
}
