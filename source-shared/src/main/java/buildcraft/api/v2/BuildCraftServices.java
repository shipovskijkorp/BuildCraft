package buildcraft.api.v2;

import buildcraft.api.v2.fuels.EnergyFluidService;
import buildcraft.api.v2.permission.PermissionServiceRegistry;
import buildcraft.api.v2.service.ServiceKey;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Stable keys for services provided by the BuildCraft API 2 runtime. */
public final class BuildCraftServices {
    public static final ServiceKey<PermissionServiceRegistry> PERMISSIONS = ServiceKey.of(id("permissions"));
    public static final ServiceKey<EnergyFluidService> ENERGY_FLUIDS = ServiceKey.of(id("energy_fluids"));

    private BuildCraftServices() {}

    private static ResourceLocation id(String path) {
        return Objects.requireNonNull(ResourceLocation.tryParse("buildcraft:" + path));
    }
}
