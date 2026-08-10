package buildcraft.api.v2.registry;

import net.minecraft.resources.ResourceLocation;

public record RegistryEntry<T>(ResourceLocation id, T value, String owner) {
}
