package buildcraft.api.v2.energy;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public record PowerLossContext(Level level, BlockPos position, MjAmount lost) {
    public PowerLossContext {
        Objects.requireNonNull(level, "level"); Objects.requireNonNull(position, "position"); Objects.requireNonNull(lost, "lost");
    }
}
