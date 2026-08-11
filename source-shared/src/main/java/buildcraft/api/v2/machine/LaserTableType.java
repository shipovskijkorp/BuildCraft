package buildcraft.api.v2.machine;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Namespaced replacement for hard-coded laser table enums. */
public record LaserTableType(ResourceLocation id) {
    public LaserTableType {
        Objects.requireNonNull(id, "id");
    }
}
