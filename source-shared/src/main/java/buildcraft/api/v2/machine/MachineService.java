package buildcraft.api.v2.machine;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface MachineService {
    Optional<MachineView> machine(Level level, BlockPos pos);
}
