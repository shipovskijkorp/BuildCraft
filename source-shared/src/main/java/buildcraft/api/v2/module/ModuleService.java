package buildcraft.api.v2.module;

import java.util.Collection;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public interface ModuleService {
    Collection<ModuleInfo> modules();
    Optional<ModuleInfo> module(ResourceLocation id);

    default boolean loaded(ResourceLocation id) {
        return module(id).map(ModuleInfo::loaded).orElse(false);
    }
}
