package buildcraft.api.v2.testkit;

import buildcraft.api.v2.ApiFeatureSet;
import buildcraft.api.v2.ApiLifecycle;
import buildcraft.api.v2.ApiRuntime;
import buildcraft.api.v2.ApiVersion;
import buildcraft.api.v2.registry.ApiRegistry;
import buildcraft.api.v2.registry.RegistryKey;
import buildcraft.api.v2.registry.SimpleApiRegistry;
import buildcraft.api.v2.service.ServiceKey;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
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
        SimpleApiRegistry<T> registry = new SimpleApiRegistry<>();
        if (registries.putIfAbsent(key.id(), registry) != null) throw new IllegalStateException("Duplicate test registry: " + key.id());
        return registry;
    }

    public <T> TestApiRuntime addService(ServiceKey<T> key, T service) {
        if (services.putIfAbsent(key, service) != null) throw new IllegalStateException("Duplicate test service: " + key.id());
        return this;
    }

    public TestApiRuntime lifecycle(ApiLifecycle lifecycle) {
        this.lifecycle = lifecycle;
        return this;
    }

    @Override @SuppressWarnings("unchecked")
    public <T> Optional<ApiRegistry<T>> registry(ResourceLocation id) { return Optional.ofNullable((ApiRegistry<T>) registries.get(id)); }
    @Override @SuppressWarnings("unchecked")
    public <T> Optional<T> service(ServiceKey<T> key) { return Optional.ofNullable((T) services.get(key)); }
    @Override public ApiVersion version() { return version; }
    @Override public ApiFeatureSet features() { return features; }
    @Override public ApiLifecycle lifecycle() { return lifecycle; }
}
