package buildcraft.api.v2.energy;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public record MjConnectionContext(
    Level level,
    BlockPos position,
    Direction side,
    MjPortDescriptor local,
    MjPortDescriptor remote
) {
    public MjConnectionContext {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(local, "local");
        Objects.requireNonNull(remote, "remote");
    }
}
