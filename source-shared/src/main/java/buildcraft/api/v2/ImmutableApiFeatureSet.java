package buildcraft.api.v2;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class ImmutableApiFeatureSet implements ApiFeatureSet {
    private final Map<ResourceLocation, ApiFeature> features;

    public ImmutableApiFeatureSet(Collection<ApiFeature> features) {
        LinkedHashMap<ResourceLocation, ApiFeature> copy = new LinkedHashMap<>();
        for (ApiFeature feature : Objects.requireNonNull(features, "features")) {
            ApiFeature previous = copy.putIfAbsent(feature.id(), feature);
            if (previous != null) throw new IllegalArgumentException("Duplicate API feature: " + feature.id());
        }
        this.features = Map.copyOf(copy);
    }

    @Override public Optional<ApiFeature> get(ResourceLocation id) { return Optional.ofNullable(features.get(id)); }
    @Override public Collection<ApiFeature> all() { return features.values(); }
}
