package buildcraft.api.v2.block;

import buildcraft.api.v2.OperationMode;
import buildcraft.api.v2.permission.AutomationActor;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public record RotationContext(Level level, BlockPos pos, BlockState state, Direction face, AutomationActor actor, OperationMode mode) {
    public RotationContext {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(face, "face");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(mode, "mode");
    }
}
