package buildcraft.api.v2.fluid;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Extensible rule used by fluid ports and data-driven filters. */
@FunctionalInterface
public interface FluidMatcher {
    boolean matches(FluidVariant variant, FluidMatchContext context);

    static FluidMatcher any() {
        return (variant, context) -> true;
    }

    static FluidMatcher none() {
        return (variant, context) -> false;
    }

    static FluidMatcher exact(FluidVariant expected) {
        Objects.requireNonNull(expected, "expected");
        return (variant, context) -> expected.equals(variant);
    }

    static FluidMatcher fluid(ResourceLocation fluidId) {
        Objects.requireNonNull(fluidId, "fluidId");
        return (variant, context) -> fluidId.equals(variant.fluidId());
    }

    static FluidMatcher tag(ResourceLocation tagId) {
        Objects.requireNonNull(tagId, "tagId");
        return (variant, context) -> Objects.requireNonNull(context, "context").isInTag(variant.fluidId(), tagId);
    }

    default FluidMatcher and(FluidMatcher other) {
        Objects.requireNonNull(other, "other");
        return (variant, context) -> matches(variant, context) && other.matches(variant, context);
    }

    default FluidMatcher or(FluidMatcher other) {
        Objects.requireNonNull(other, "other");
        return (variant, context) -> matches(variant, context) || other.matches(variant, context);
    }

    default FluidMatcher negate() {
        return (variant, context) -> !matches(variant, context);
    }
}
