package buildcraft.api.v2.fuels;

import buildcraft.api.v2.fluid.FluidMatchContext;
import buildcraft.api.v2.fluid.FluidMatcher;
import buildcraft.api.v2.fluid.FluidVariant;
import buildcraft.api.v2.persistence.ApiCodec;
import buildcraft.api.v2.persistence.CodecResult;
import java.util.Objects;
import java.util.Optional;

/** Immutable fluid-coolant definition. */
public final class CoolantProfile implements EnergyFluidDefinition {
    public static final ApiCodec<CoolantProfile, CoolantProfileData> CODEC = new ApiCodec<>() {
        @Override
        public CodecResult<CoolantProfile> decode(CoolantProfileData data) {
            if (data == null) return CodecResult.failure("Coolant profile payload is null");
            CodecResult<FluidSelector> selector = FluidSelector.CODEC.decode(data.selector());
            if (!selector.successful()) return CodecResult.failure(String.join("; ", selector.errors()));
            try {
                return CodecResult.success(CoolantProfile.constant(selector.valueOrThrow(), data.degreesPerMilliBucket()));
            } catch (RuntimeException ex) {
                return CodecResult.failure(safeMessage(ex));
            }
        }

        @Override
        public CodecResult<CoolantProfileData> encode(CoolantProfile value) {
            if (value == null) return CodecResult.failure("Coolant profile is null");
            if (!(value.matcher instanceof FluidSelector selector)) {
                return CodecResult.failure("Programmatic coolant matcher is not data-serializable");
            }
            if (!(value.coolingRule instanceof ConstantCoolingRule constant)) {
                return CodecResult.failure("Programmatic coolant cooling rule is not data-serializable");
            }
            CodecResult<FluidSelectorData> encoded = FluidSelector.CODEC.encode(selector);
            return encoded.successful()
                ? CodecResult.success(new CoolantProfileData(encoded.valueOrThrow(), constant.degreesPerMilliBucket()))
                : CodecResult.failure(String.join("; ", encoded.errors()));
        }

        private String safeMessage(RuntimeException ex) {
            String message = ex.getMessage();
            return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
        }
    };

    private final FluidMatcher matcher;
    private final CoolingRule coolingRule;

    public CoolantProfile(FluidMatcher matcher, CoolingRule coolingRule) {
        this.matcher = Objects.requireNonNull(matcher, "matcher");
        this.coolingRule = Objects.requireNonNull(coolingRule, "coolingRule");
    }

    public static CoolantProfile constant(FluidMatcher matcher, double degreesPerMilliBucket) {
        return new CoolantProfile(matcher, new ConstantCoolingRule(degreesPerMilliBucket));
    }

    @Override
    public Kind kind() { return Kind.COOLANT; }
    public FluidMatcher matcher() { return matcher; }
    public CoolingRule coolingRule() { return coolingRule; }

    public boolean matches(FluidVariant fluid, FluidMatchContext context) {
        return matcher.matches(fluid, context);
    }

    public double degreesPerMilliBucket(FluidVariant fluid, double heat) {
        double result = coolingRule.degreesPerMilliBucket(fluid, heat);
        return Double.isFinite(result) && result > 0 ? result : 0;
    }

    public Optional<FluidVariant> representativeVariant() {
        return matcher instanceof FluidSelector selector ? selector.representativeVariant() : Optional.empty();
    }
}
