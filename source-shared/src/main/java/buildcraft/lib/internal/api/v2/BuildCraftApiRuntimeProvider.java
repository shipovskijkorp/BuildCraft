package buildcraft.lib.internal.api.v2;

import buildcraft.api.v2.ApiFeatureSet;
import buildcraft.api.v2.ApiLifecycle;
import buildcraft.api.v2.ApiRuntime;
import buildcraft.api.v2.ApiVersion;
import buildcraft.api.v2.registry.ApiRegistry;
import buildcraft.api.v2.service.ServiceKey;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * Internal ServiceLoader bridge from the public {@link ApiRuntime} contract to
 * the singleton BCCE Lib runtime. This class is public only because Java's
 * classpath ServiceLoader needs a public provider constructor; it is not addon API.
 */
public final class BuildCraftApiRuntimeProvider implements ApiRuntime {
    public BuildCraftApiRuntimeProvider() {
    }

    BuildCraftApiRuntime delegate() {
        return BuildCraftApiRuntime.INSTANCE;
    }

    @Override
    public <T> Optional<ApiRegistry<T>> registry(ResourceLocation id) {
        return delegate().registry(id);
    }

    @Override
    public <T> Optional<T> service(ServiceKey<T> key) {
        return delegate().service(key);
    }

    @Override public ApiVersion version() { return delegate().version(); }
    @Override public ApiFeatureSet features() { return delegate().features(); }
    @Override public ApiLifecycle lifecycle() { return delegate().lifecycle(); }
}
