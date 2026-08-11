package buildcraft.api.v2;

import buildcraft.api.v2.registry.ApiRegistry;
import buildcraft.api.v2.registry.RegistryKey;
import buildcraft.api.v2.service.ServiceKey;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Main entry point for BuildCraft Extension API 2.
 *
 * API v2 intentionally exposes only loader-neutral contracts.
 */
public final class BuildCraftApi {
    public static final ApiVersion VERSION = new ApiVersion(2, 0, 0);
    private static final ApiRuntime EMPTY = ApiRuntime.empty();
    private static final AtomicReference<ApiRuntime> RUNTIME = new AtomicReference<>(EMPTY);

    private BuildCraftApi() {
    }

    public static ApiRuntime runtime() {
        return RUNTIME.get();
    }

    public static <T> ApiRegistry<T> registry(RegistryKey<T> key) {
        return runtime().requireRegistry(key);
    }

    public static <T> T service(ServiceKey<T> key) {
        return runtime().requireService(key);
    }

    /**
     * Installs the runtime exactly once during BuildCraft bootstrap.
     */
    public static void install(ApiRuntime newRuntime) {
        Objects.requireNonNull(newRuntime, "newRuntime");
        if (!RUNTIME.compareAndSet(EMPTY, newRuntime)) {
            throw new IllegalStateException("BuildCraft API runtime already installed");
        }
    }
}
