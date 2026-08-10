package buildcraft.api.v2;

import buildcraft.api.v2.crops.CropService;
import buildcraft.api.v2.facade.FacadeRuleService;
import buildcraft.api.v2.fuels.EnergyFluidService;
import buildcraft.api.v2.recipe.MachineRecipeService;
import buildcraft.api.v2.template.TemplateService;
import buildcraft.api.v2.world.WorldPropertyService;
import buildcraft.api.v2.permission.PermissionServiceRegistry;
import buildcraft.api.v2.service.ServiceKey;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Stable keys for services provided by the BuildCraft API 2 runtime. */
public final class BuildCraftServices {
    public static final ServiceKey<PermissionServiceRegistry> PERMISSIONS = ServiceKey.of(id("permissions"));
    public static final ServiceKey<EnergyFluidService> ENERGY_FLUIDS = ServiceKey.of(id("energy_fluids"));
    public static final ServiceKey<MachineRecipeService> MACHINE_RECIPES = ServiceKey.of(id("machine_recipes"));
    public static final ServiceKey<CropService> CROPS = ServiceKey.of(id("crops"));
    public static final ServiceKey<TemplateService> TEMPLATES = ServiceKey.of(id("templates"));
    public static final ServiceKey<FacadeRuleService> FACADE_RULES = ServiceKey.of(id("facade_rules"));
    public static final ServiceKey<WorldPropertyService> WORLD_PROPERTIES = ServiceKey.of(id("world_properties"));

    private BuildCraftServices() {}

    private static ResourceLocation id(String path) {
        return Objects.requireNonNull(ResourceLocation.tryParse("buildcraft:" + path));
    }
}
