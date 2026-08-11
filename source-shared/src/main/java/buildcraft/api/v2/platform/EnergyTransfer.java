package buildcraft.api.v2.platform;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Platform bridge that exposes the loader's external energy API through ExternalEnergyPort. */
public interface EnergyTransfer {
    Optional<ExternalEnergyPort> find(Level level, BlockPos pos, Direction side);
}
