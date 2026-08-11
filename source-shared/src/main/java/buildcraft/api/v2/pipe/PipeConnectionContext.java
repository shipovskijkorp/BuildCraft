package buildcraft.api.v2.pipe;

import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public record PipeConnectionContext(Level level, PipeView pipe, Direction side, BlockState neighbourState) {
    public PipeConnectionContext {
        Objects.requireNonNull(level, "level"); Objects.requireNonNull(pipe, "pipe");
        Objects.requireNonNull(side, "side"); Objects.requireNonNull(neighbourState, "neighbourState");
    }
}
