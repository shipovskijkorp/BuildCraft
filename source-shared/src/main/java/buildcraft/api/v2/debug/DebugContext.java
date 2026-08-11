package buildcraft.api.v2.debug;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public record DebugContext(Level level, BlockPos position, Optional<Direction> side, boolean clientSide) {
    public DebugContext {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(position, "position");
        side = Objects.requireNonNull(side, "side");
    }
}
