package buildcraft.api.v2.testkit;

import buildcraft.api.v2.ApiFeatureSet;
import buildcraft.api.v2.ApiLifecycle;
import buildcraft.api.v2.ApiRuntime;
import buildcraft.api.v2.ApiVersion;
import buildcraft.api.v2.registry.ApiRegistry;
import buildcraft.api.v2.registry.RegistryKey;
import buildcraft.api.v2.service.ServiceKey;
import buildcraft.api.v2.registry.RegistrationContext;
import buildcraft.api.v2.registry.RegistryEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** Small in-memory runtime for addon unit tests. It is never installed globally. */
public final class TestApiRuntime implements ApiRuntime {
    private final Map<ResourceLocation, ApiRegistry<?>> registries = new LinkedHashMap<>();
    private final Map<ServiceKey<?>, Object> services = new LinkedHashMap<>();
    private final ApiVersion version;
    private final ApiFeatureSet features;
    private ApiLifecycle lifecycle = ApiLifecycle.DISCOVERY;

    public TestApiRuntime(ApiVersion version, ApiFeatureSet features) {
        this.version = version;
        this.features = features;
    }

    public <T> ApiRegistry<T> addRegistry(RegistryKey<T> key) {
        ApiRegistry<T> registry = new TestRegistry<>();
        if (registries.putIfAbsent(key.id(), registry) != null) throw new IllegalStateException("Duplicate test registry: " + key.id());
        return registry;
    }

    public <T> TestApiRuntime addService(ServiceKey<T> key, T service) {
        if (services.putIfAbsent(key, service) != null) throw new IllegalStateException("Duplicate test service: " + key.id());
        return this;
    }

    public TestApiRuntime lifecycle(ApiLifecycle lifecycle) {
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
        if (lifecycle == ApiLifecycle.FROZEN) {
            for (ApiRegistry<?> registry : registries.values()) {
                ((TestRegistry<?>) registry).freezeForTest();
            }
        }
        return this;
    }

    @Override @SuppressWarnings("unchecked")
    public <T> Optional<ApiRegistry<T>> registry(ResourceLocation id) { return Optional.ofNullable((ApiRegistry<T>) registries.get(id)); }
    @Override @SuppressWarnings("unchecked")
    public <T> Optional<T> service(ServiceKey<T> key) { return Optional.ofNullable((T) services.get(key)); }
    @Override public ApiVersion version() { return version; }
    @Override public ApiFeatureSet features() { return features; }
    @Override public ApiLifecycle lifecycle() { return lifecycle; }

    private static final class TestRegistry<T> implements ApiRegistry<T> {
        private final Map<ResourceLocation, RegistryEntry<T>> entries = new LinkedHashMap<>();
        private final Map<ResourceLocation, ResourceLocation> aliases = new LinkedHashMap<>();
        private boolean frozen;

        @Override
        public void register(ResourceLocation id, T value) {
            register(id, value, () -> "test-addon");
        }

        @Override
        public void register(ResourceLocation id, T value, RegistrationContext context) {
            ensureMutable();
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(value, "value");
            String owner = Objects.requireNonNull(context, "context").owner();
            if (owner == null || owner.isBlank()) throw new IllegalArgumentException("registration owner must not be blank");
            if (entries.containsKey(id) || aliases.containsKey(id)) throw new IllegalStateException("Duplicate registry id or alias: " + id);
            entries.put(id, new RegistryEntry<>(id, value, owner));
        }

        @Override
        public void registerAlias(ResourceLocation alias, ResourceLocation canonicalId, RegistrationContext context) {
            ensureMutable();
            Objects.requireNonNull(alias, "alias");
            Objects.requireNonNull(canonicalId, "canonicalId");
            Objects.requireNonNull(context, "context");
            if (alias.equals(canonicalId)) throw new IllegalArgumentException("alias must differ from canonicalId");
            if (entries.containsKey(alias) || aliases.putIfAbsent(alias, canonicalId) != null) {
                throw new IllegalStateException("Duplicate registry id or alias: " + alias);
            }
        }

        @Override public T get(ResourceLocation id) { RegistryEntry<T> entry = entries.get(canonicalId(id)); return entry == null ? null : entry.value(); }
        @Override public Optional<RegistryEntry<T>> entry(ResourceLocation id) { return Optional.ofNullable(entries.get(canonicalId(id))); }

        @Override
        public ResourceLocation canonicalId(ResourceLocation id) {
            ResourceLocation current = Objects.requireNonNull(id, "id");
            Set<ResourceLocation> visited = new LinkedHashSet<>();
            while (aliases.containsKey(current)) {
                if (!visited.add(current)) throw new IllegalStateException("Registry alias cycle starting at " + id);
                current = aliases.get(current);
            }
            return current;
        }

        @Override public Collection<T> values() {
            ArrayList<T> values = new ArrayList<>(entries.size());
            for (RegistryEntry<T> entry : entries.values()) values.add(entry.value());
            return Collections.unmodifiableList(values);
        }
        @Override public Collection<RegistryEntry<T>> entries() { return Collections.unmodifiableCollection(entries.values()); }
        @Override public boolean frozen() { return frozen; }
        private void freezeForTest() {
            if (frozen) return;
            for (ResourceLocation alias : aliases.keySet()) {
                ResourceLocation canonical = canonicalId(alias);
                if (!entries.containsKey(canonical)) throw new IllegalStateException("Registry alias " + alias + " targets missing id " + canonical);
            }
            frozen = true;
        }
        private void ensureMutable() { if (frozen) throw new IllegalStateException("Registry is frozen"); }
    }

}
