package buildcraft.api.v2.machine;

import java.util.Set;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record MachineType(ResourceLocation id, Set<ResourceLocation> components) {
    public MachineType {
        Objects.requireNonNull(id, "id"); components = Set.copyOf(Objects.requireNonNull(components, "components"));
    }
}
