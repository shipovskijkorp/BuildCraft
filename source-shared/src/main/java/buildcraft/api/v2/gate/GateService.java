package buildcraft.api.v2.gate;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public interface GateService {
    Optional<GateView> gate(Level level, BlockPos pos, Direction side);
}
