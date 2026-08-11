package buildcraft.api.v2.gate;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public interface GateView {
    BlockPos position();
    Direction side();
    GateProgram program();
    Optional<GateControl> control();
}
