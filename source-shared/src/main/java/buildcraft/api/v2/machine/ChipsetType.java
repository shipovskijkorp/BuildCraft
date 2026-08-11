package buildcraft.api.v2.machine;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record ChipsetType(ResourceLocation id, int tier) {
    public ChipsetType {
        Objects.requireNonNull(id, "id");
        if (tier < 0) throw new IllegalArgumentException("tier must be non-negative");
    }
}
