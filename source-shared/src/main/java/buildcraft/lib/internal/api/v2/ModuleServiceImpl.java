package buildcraft.lib.internal.api.v2;

import buildcraft.api.v2.module.ModuleInfo;
import buildcraft.api.v2.module.ModuleService;
import buildcraft.lib.internal.core.BuildCraftAPI;
import buildcraft.lib.internal.module.BCModules;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/** API2 read-only view over the physical BuildCraft module containers. */
final class ModuleServiceImpl implements ModuleService {
    @Override
    public Collection<ModuleInfo> modules() {
        List<ModuleInfo> out = new ArrayList<>(BCModules.VALUES.length);
        for (BCModules module : BCModules.VALUES) {
            out.add(info(module));
        }
        return List.copyOf(out);
    }

    @Override
    public Optional<ModuleInfo> module(ResourceLocation id) {
        if (!"buildcraft".equals(id.getNamespace())) return Optional.empty();
        for (BCModules module : BCModules.VALUES) {
            if (module.lowerCaseName.equals(id.getPath())) {
                return Optional.of(info(module));
            }
        }
        return Optional.empty();
    }

    private static ModuleInfo info(BCModules module) {
        ResourceLocation id = ResourceLocation.tryParse("buildcraft:" + module.lowerCaseName);
        if (id == null) throw new IllegalStateException("Invalid BuildCraft module id: " + module.lowerCaseName);
        boolean loaded = module.isLoaded();
        return new ModuleInfo(id, loaded ? BuildCraftAPI.getVersion() : "missing", loaded);
    }
}
