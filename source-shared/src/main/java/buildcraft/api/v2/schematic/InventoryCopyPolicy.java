package buildcraft.api.v2.schematic;

import java.util.Set;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record InventoryCopyPolicy(ResourceLocation id, Set<DataPath> allowedPaths, Set<DataPath> deniedPaths) {
    public InventoryCopyPolicy {
        Objects.requireNonNull(id, "id");
        allowedPaths = Set.copyOf(Objects.requireNonNull(allowedPaths, "allowedPaths"));
        deniedPaths = Set.copyOf(Objects.requireNonNull(deniedPaths, "deniedPaths"));
    }
}
