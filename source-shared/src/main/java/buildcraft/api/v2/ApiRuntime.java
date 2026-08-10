package buildcraft.api.v2;

import buildcraft.api.v2.registry.ApiRegistry;
import buildcraft.api.v2.service.ServiceKey;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/** Immutable API runtime facade. */
public interface ApiRuntime {
    ApiRuntime EMPTY = new ApiRuntime() {
        @Override
        public <T> Optional<ApiRegistry<T>> registry(ResourceLocation id) {
            return Optional.empty();
        }

        @Override
        public <T> Optional<T> service(ServiceKey<T> key) {
            return Optional.empty();
        }
    };

    static ApiRuntime empty() {
        return EMPTY;
    }

    <T> Optional<ApiRegistry<T>> registry(ResourceLocation id);

    <T> Optional<T> service(ServiceKey<T> key);

    default <T> T requireService(ServiceKey<T> key) {
        return service(key).orElseThrow(() -> new IllegalStateException("BuildCraft API service is unavailable: " + key.id()));
    }
}
