package buildcraft.api.v2.registry;

import net.minecraft.resources.ResourceLocation;

public interface RegistryBuilder<T> {
    RegistryBuilder<T> register(ResourceLocation id, T value, RegistrationContext context);

    RegistrySnapshot<T> freeze();
}
