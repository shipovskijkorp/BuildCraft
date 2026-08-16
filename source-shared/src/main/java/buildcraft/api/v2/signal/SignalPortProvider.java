package buildcraft.api.v2.signal;

import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * Optional block-entity extension point for attaching API2 signal endpoints to adjacent BuildCraft pipe wires.
 * The supplied side is the face of the block entity on which the wire is attached.
 */
public interface SignalPortProvider {
    Optional<? extends SignalPort<?>> signalPort(Direction side, ResourceLocation channelId);
}
