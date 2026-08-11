package buildcraft.api.v2.machine;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * Immutable composition definition for a BuildCraft-style machine.
 *
 * <p>Machine variants are created by copying a registered base definition, overriding only
 * the desired properties/components, and registering the resulting definition under a new id.
 * This intentionally avoids subclassing BuildCraft block entities.
 */
public final class MachineType {
    private final ResourceLocation id;
    private final Set<ResourceLocation> components;
    private final Map<MachineProperty<?>, Object> properties;

    /** Compatibility constructor for simple component-only definitions. */
    public MachineType(ResourceLocation id, Set<ResourceLocation> components) {
        this(id, components, Map.of());
    }

    private MachineType(
        ResourceLocation id,
        Set<ResourceLocation> components,
        Map<MachineProperty<?>, Object> properties
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.components = Collections.unmodifiableSet(new LinkedHashSet<>(Objects.requireNonNull(components, "components")));
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(properties, "properties")));
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public static Builder variant(ResourceLocation id, MachineType base) {
        return new Builder(id).copyFrom(base);
    }

    public Builder copyAs(ResourceLocation newId) {
        return variant(newId, this);
    }

    public ResourceLocation id() {
        return id;
    }

    public Set<ResourceLocation> components() {
        return components;
    }

    public Map<MachineProperty<?>, Object> properties() {
        return properties;
    }

    public <T> Optional<T> property(MachineProperty<T> property) {
        Objects.requireNonNull(property, "property");
        Object value = properties.get(property);
        return value == null ? Optional.empty() : Optional.of(property.cast(value));
    }

    public <T> T propertyOrDefault(MachineProperty<T> property, T fallback) {
        return property(property).orElse(fallback);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof MachineType that
            && id.equals(that.id)
            && components.equals(that.components)
            && properties.equals(that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, components, properties);
    }

    @Override
    public String toString() {
        return "MachineType[" + id + ", components=" + components + ", properties=" + properties + "]";
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final Set<ResourceLocation> components = new LinkedHashSet<>();
        private final Map<MachineProperty<?>, Object> properties = new LinkedHashMap<>();

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder copyFrom(MachineType base) {
            Objects.requireNonNull(base, "base");
            components.clear();
            components.addAll(base.components);
            properties.clear();
            properties.putAll(base.properties);
            return this;
        }

        public Builder component(ResourceLocation componentId) {
            components.add(Objects.requireNonNull(componentId, "componentId"));
            return this;
        }

        public Builder component(MachineComponentType<?> componentType) {
            return component(Objects.requireNonNull(componentType, "componentType").id());
        }

        public Builder removeComponent(ResourceLocation componentId) {
            components.remove(Objects.requireNonNull(componentId, "componentId"));
            return this;
        }

        public Builder clearComponents() {
            components.clear();
            return this;
        }

        public <T> Builder property(MachineProperty<T> property, T value) {
            MachineProperty<T> checkedProperty = Objects.requireNonNull(property, "property");
            properties.put(checkedProperty, checkedProperty.validate(value));
            return this;
        }

        public Builder removeProperty(MachineProperty<?> property) {
            properties.remove(Objects.requireNonNull(property, "property"));
            return this;
        }

        public MachineType build() {
            return new MachineType(id, components, properties);
        }
    }
}
