package buildcraft.lib.api.v2;

import buildcraft.api.v2.ApiRuntime;
import buildcraft.api.v2.BuildCraftApi;
import buildcraft.api.v2.BuildCraftServices;
import buildcraft.api.v2.registry.ApiRegistry;
import buildcraft.api.v2.service.ServiceKey;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/** Internal runtime implementation. Public callers see only ApiRuntime contracts. */
public final class BuildCraftApiRuntime implements ApiRuntime {
    public static final BuildCraftApiRuntime INSTANCE = new BuildCraftApiRuntime();

    private final Map<ResourceLocation, ApiRegistry<?>> registries = new LinkedHashMap<>();
    private final Map<ServiceKey<?>, Object> services = new LinkedHashMap<>();
    private final PermissionServiceRegistryImpl permissions = new PermissionServiceRegistryImpl();
    private final EnergyFluidRegistryImpl energyFluids = new EnergyFluidRegistryImpl();

    private BuildCraftApiRuntime() {
        services.put(BuildCraftServices.PERMISSIONS, permissions);
        services.put(BuildCraftServices.ENERGY_FLUIDS, energyFluids);
    }

    public static synchronized void install() {
        if (BuildCraftApi.runtime() == ApiRuntime.empty()) {
            BuildCraftApi.install(INSTANCE);
        } else if (BuildCraftApi.runtime() != INSTANCE) {
            throw new IllegalStateException("A foreign BuildCraft API runtime was installed before BuildCraft bootstrap");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<ApiRegistry<T>> registry(ResourceLocation id) {
        return Optional.ofNullable((ApiRegistry<T>) registries.get(id));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> service(ServiceKey<T> key) {
        return Optional.ofNullable((T) services.get(key));
    }

    public EnergyFluidRegistryImpl energyFluids() {
        return energyFluids;
    }
}
