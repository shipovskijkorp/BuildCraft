package buildcraft.api.v2.block;

import buildcraft.api.v2.OperationMode;
import buildcraft.api.v2.permission.AutomationActor;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public record PaintContext(Level level, BlockPos pos, BlockState state, DyeColor color, AutomationActor actor, OperationMode mode) {
    public PaintContext {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(mode, "mode");
    }
}
