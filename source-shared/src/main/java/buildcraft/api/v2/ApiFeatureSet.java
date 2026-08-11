package buildcraft.api.v2;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/** Immutable feature view used for capability negotiation between addons and BCCE. */
public interface ApiFeatureSet {
    ApiFeatureSet EMPTY = new ApiFeatureSet() {
        @Override public Optional<ApiFeature> get(ResourceLocation id) { return Optional.empty(); }
        @Override public Collection<ApiFeature> all() { return List.of(); }
    };

    Optional<ApiFeature> get(ResourceLocation id);
    Collection<ApiFeature> all();

    default boolean supports(ResourceLocation id) {
        return supports(id, 1);
    }

    default boolean supports(ResourceLocation id, int minimumLevel) {
        if (minimumLevel < 1) throw new IllegalArgumentException("minimumLevel must be >= 1");
        return get(id).map(feature -> feature.level() >= minimumLevel).orElse(false);
    }
}
