package buildcraft.api.v2.robot;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/** A typed docking-station view. Custom types may resolve their own port from the public dock context. */
public record DockPortType<T>(ResourceLocation id, Class<T> portType, DockPortResolver<T> resolver) {
    public DockPortType {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(portType, "portType");
        Objects.requireNonNull(resolver, "resolver");
    }

    public DockPortType(ResourceLocation id, Class<T> portType) {
        this(id, portType, ignored -> Optional.empty());
    }

    public Optional<T> resolve(RobotDockContext context) {
        return resolver.resolve(Objects.requireNonNull(context, "context"));
    }
}
