package buildcraft.api.v2.robot;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Loader-neutral context supplied when resolving an addon-defined docking-station port. */
public record RobotDockContext(Level level, BlockPos position, Optional<Direction> side, boolean occupied) {
    public RobotDockContext {
        Objects.requireNonNull(level, "level");
        position = Objects.requireNonNull(position, "position").immutable();
        side = Objects.requireNonNull(side, "side");
    }
}
