package buildcraft.api.v2.platform;

import buildcraft.api.v2.fluid.FluidPort;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Platform bridge that exposes loader-native fluid handlers as FluidPort. */
public interface FluidTransfer {
    Optional<FluidPort> find(Level level, BlockPos pos, Direction side);
}
