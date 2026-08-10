package buildcraft.api.v2.fuels;

import buildcraft.api.v2.fluid.FluidMatchContext;
import buildcraft.api.v2.fluid.FluidMatcher;
import buildcraft.api.v2.fluid.FluidVariant;
import buildcraft.api.v2.persistence.ApiCodec;
import buildcraft.api.v2.persistence.CodecResult;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * Serializable subset of {@link FluidMatcher} used by data-driven API domains.
 * Programmatic profiles may still provide arbitrary FluidMatcher implementations.
 */
public final class FluidSelector implements FluidMatcher {
    public static final ApiCodec<FluidSelector, FluidSelectorData> CODEC = new ApiCodec<>() {
        @Override
        public CodecResult<FluidSelector> decode(FluidSelectorData payload) {
            if (payload == null || payload.kind() == null) {
                return CodecResult.failure("Fluid selector payload/kind is null");
            }
            try {
                return switch (payload.kind()) {
                    case FLUID -> CodecResult.success(fluid(requireId(payload)));
                    case TAG -> CodecResult.success(tag(requireId(payload)));
                    case EXACT_VARIANT -> {
                        if (payload.variant() == null) yield CodecResult.failure("Exact fluid selector is missing variant");
                        yield FluidVariant.CODEC.decode(payload.variant()).map(FluidSelector::exact);
                    }
                };
            } catch (RuntimeException ex) {
                return CodecResult.failure(safeMessage(ex));
            }
        }

        @Override
        public CodecResult<FluidSelectorData> encode(FluidSelector value) {
            if (value == null) return CodecResult.failure("Fluid selector is null");
            return switch (value.kind) {
                case FLUID -> CodecResult.success(new FluidSelectorData(FluidSelectorData.Kind.FLUID, value.id, null));
                case TAG -> CodecResult.success(new FluidSelectorData(FluidSelectorData.Kind.TAG, value.id, null));
                case EXACT_VARIANT -> FluidVariant.CODEC.encode(value.variant)
                    .map(data -> new FluidSelectorData(FluidSelectorData.Kind.EXACT_VARIANT, null, data));
            };
        }

        private ResourceLocation requireId(FluidSelectorData payload) {
            if (payload.id() == null) throw new IllegalArgumentException(payload.kind() + " selector is missing id");
            return payload.id();
        }

        private String safeMessage(RuntimeException ex) {
            String message = ex.getMessage();
            return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
        }
    };

    private enum Kind { FLUID, TAG, EXACT_VARIANT }

    private final Kind kind;
    private final ResourceLocation id;
    private final FluidVariant variant;

    private FluidSelector(Kind kind, ResourceLocation id, FluidVariant variant) {
        this.kind = kind;
        this.id = id;
        this.variant = variant;
    }

    public static FluidSelector fluid(ResourceLocation fluidId) {
        return new FluidSelector(Kind.FLUID, Objects.requireNonNull(fluidId, "fluidId"), null);
    }

    public static FluidSelector tag(ResourceLocation tagId) {
        return new FluidSelector(Kind.TAG, Objects.requireNonNull(tagId, "tagId"), null);
    }

    public static FluidSelector exact(FluidVariant variant) {
        return new FluidSelector(Kind.EXACT_VARIANT, null, Objects.requireNonNull(variant, "variant"));
    }

    @Override
    public boolean matches(FluidVariant candidate, FluidMatchContext context) {
        Objects.requireNonNull(candidate, "candidate");
        return switch (kind) {
            case FLUID -> id.equals(candidate.fluidId());
            case TAG -> Objects.requireNonNull(context, "context").isInTag(candidate.fluidId(), id);
            case EXACT_VARIANT -> variant.equals(candidate);
        };
    }

    public Optional<ResourceLocation> fluidId() {
        return kind == Kind.FLUID ? Optional.of(id) : Optional.empty();
    }

    public Optional<ResourceLocation> tagId() {
        return kind == Kind.TAG ? Optional.of(id) : Optional.empty();
    }

    public Optional<FluidVariant> exactVariant() {
        return kind == Kind.EXACT_VARIANT ? Optional.of(variant) : Optional.empty();
    }

    /** A concrete representative when this selector has one. Tags intentionally do not. */
    public Optional<FluidVariant> representativeVariant() {
        if (kind == Kind.FLUID) return Optional.of(FluidVariant.of(id));
        if (kind == Kind.EXACT_VARIANT) return Optional.of(variant);
        return Optional.empty();
    }
}
