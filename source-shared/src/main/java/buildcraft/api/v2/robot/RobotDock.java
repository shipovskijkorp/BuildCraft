package buildcraft.api.v2.robot;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Stable read-only view of a robot docking station. */
public interface RobotDock {
    BlockPos position();

    /** Side used by sided stations such as pipe plugs; empty for unsided/block stations. */
    Optional<Direction> side();

    <T> Optional<T> port(DockPortType<T> type);
    boolean occupied();
}
