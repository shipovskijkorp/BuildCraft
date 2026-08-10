package buildcraft.api.v2.registry;

import java.util.Collection;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public interface RegistrySnapshot<T> {
    Optional<T> get(ResourceLocation id);

    Collection<RegistryEntry<T>> entries();
}
