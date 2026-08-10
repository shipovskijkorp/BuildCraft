package buildcraft.api.v2.registry;

import net.minecraft.resources.ResourceLocation;

public record RegistryKey<T>(ResourceLocation id) {
    public RegistryKey {
        if (id == null) throw new NullPointerException("id");
    }
}
