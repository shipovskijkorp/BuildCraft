package buildcraft.api.v2.robot;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public interface RobotDock {
    BlockPos position();
    Direction side();
    <T> Optional<T> port(DockPortType<T> type);
    boolean occupied();
}
