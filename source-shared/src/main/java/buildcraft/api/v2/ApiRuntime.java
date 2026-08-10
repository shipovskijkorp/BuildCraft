package buildcraft.api.v2;

import buildcraft.api.v2.registry.ApiRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * Immutable API runtime facade.
 */
public interface ApiRuntime {
    ApiRuntime EMPTY = new ApiRuntime() {
        @Override
        public <T> Optional<ApiRegistry<T>> registry(ResourceLocation id) {
            return Optional.empty();
        }
    };

    static ApiRuntime empty() {
        return EMPTY;
    }

    <T> Optional<ApiRegistry<T>> registry(ResourceLocation id);
}
