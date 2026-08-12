package buildcraft.api.v2.machine;

import buildcraft.api.v2.energy.MjPort;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/** Target that accepts laser MJ through a dedicated, transaction-safe port. */
public interface LaserTarget {
    MjPort laserPort();
    default Optional<ResourceLocation> typeId() { return Optional.empty(); }
}
