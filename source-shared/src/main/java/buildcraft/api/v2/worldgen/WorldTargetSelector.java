package buildcraft.api.v2.worldgen;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;

/**
 * Data-like world selection used by BuildCraft world-generation extension rules.
 *
 * <p>Empty inclusion sets mean "all". Exclusions always win. Tag membership is supplied by
 * the runtime so this contract stays independent from loader-specific tag lookup APIs.
 */
public final class WorldTargetSelector {
    public static final WorldTargetSelector ALL = builder().build();

    private final Set<ResourceLocation> dimensions;
    private final Set<ResourceLocation> dimensionTags;
    private final Set<ResourceLocation> excludedDimensions;
    private final Set<ResourceLocation> biomes;
    private final Set<ResourceLocation> biomeTags;
    private final Set<ResourceLocation> excludedBiomes;

    private WorldTargetSelector(Builder builder) {
        dimensions = immutable(builder.dimensions);
        dimensionTags = immutable(builder.dimensionTags);
        excludedDimensions = immutable(builder.excludedDimensions);
        biomes = immutable(builder.biomes);
        biomeTags = immutable(builder.biomeTags);
        excludedBiomes = immutable(builder.excludedBiomes);
    }

    public static Builder builder() { return new Builder(); }

    public Set<ResourceLocation> dimensions() { return dimensions; }
    public Set<ResourceLocation> dimensionTags() { return dimensionTags; }
    public Set<ResourceLocation> excludedDimensions() { return excludedDimensions; }
    public Set<ResourceLocation> biomes() { return biomes; }
    public Set<ResourceLocation> biomeTags() { return biomeTags; }
    public Set<ResourceLocation> excludedBiomes() { return excludedBiomes; }

    public boolean matches(
        ResourceLocation dimension,
        ResourceLocation biome,
        Predicate<ResourceLocation> dimensionTagMembership,
        Predicate<ResourceLocation> biomeTagMembership
    ) {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(biome, "biome");
        Objects.requireNonNull(dimensionTagMembership, "dimensionTagMembership");
        Objects.requireNonNull(biomeTagMembership, "biomeTagMembership");

        if (excludedDimensions.contains(dimension) || excludedBiomes.contains(biome)) return false;
        boolean dimensionIncluded = dimensions.isEmpty() && dimensionTags.isEmpty()
            || dimensions.contains(dimension)
            || dimensionTags.stream().anyMatch(dimensionTagMembership);
        if (!dimensionIncluded) return false;
        return biomes.isEmpty() && biomeTags.isEmpty()
            || biomes.contains(biome)
            || biomeTags.stream().anyMatch(biomeTagMembership);
    }

    private static Set<ResourceLocation> immutable(Set<ResourceLocation> source) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(source));
    }

    public static final class Builder {
        private final Set<ResourceLocation> dimensions = new LinkedHashSet<>();
        private final Set<ResourceLocation> dimensionTags = new LinkedHashSet<>();
        private final Set<ResourceLocation> excludedDimensions = new LinkedHashSet<>();
        private final Set<ResourceLocation> biomes = new LinkedHashSet<>();
        private final Set<ResourceLocation> biomeTags = new LinkedHashSet<>();
        private final Set<ResourceLocation> excludedBiomes = new LinkedHashSet<>();

        public Builder dimension(ResourceLocation id) { dimensions.add(Objects.requireNonNull(id, "id")); return this; }
        public Builder dimensionTag(ResourceLocation id) { dimensionTags.add(Objects.requireNonNull(id, "id")); return this; }
        public Builder excludeDimension(ResourceLocation id) { excludedDimensions.add(Objects.requireNonNull(id, "id")); return this; }
        public Builder biome(ResourceLocation id) { biomes.add(Objects.requireNonNull(id, "id")); return this; }
        public Builder biomeTag(ResourceLocation id) { biomeTags.add(Objects.requireNonNull(id, "id")); return this; }
        public Builder excludeBiome(ResourceLocation id) { excludedBiomes.add(Objects.requireNonNull(id, "id")); return this; }
        public WorldTargetSelector build() { return new WorldTargetSelector(this); }
    }
}
