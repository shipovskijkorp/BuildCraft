package buildcraft.api.v2;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Main entry point for BuildCraft Extension API 2.
 *
 * API v2 intentionally exposes only loader-neutral contracts.
 */
public final class BuildCraftApi {
    private static final ApiRuntime EMPTY = ApiRuntime.empty();
    private static final AtomicReference<ApiRuntime> RUNTIME = new AtomicReference<>(EMPTY);

    private BuildCraftApi() {
    }

    public static ApiRuntime runtime() {
        return RUNTIME.get();
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
