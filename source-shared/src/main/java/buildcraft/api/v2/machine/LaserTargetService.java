package buildcraft.api.v2.machine;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public interface LaserTargetService {
    Optional<LaserTarget> target(Level level, BlockPos pos, Direction side);
}
