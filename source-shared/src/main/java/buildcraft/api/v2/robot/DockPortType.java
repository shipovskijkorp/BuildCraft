package buildcraft.api.v2.robot;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record DockPortType<T>(ResourceLocation id, Class<T> portType) {
    public DockPortType {
        Objects.requireNonNull(id, "id"); Objects.requireNonNull(portType, "portType");
    }
}
