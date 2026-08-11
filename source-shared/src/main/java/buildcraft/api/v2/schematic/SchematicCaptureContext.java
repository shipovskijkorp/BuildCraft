package buildcraft.api.v2.schematic;

import buildcraft.api.v2.permission.AutomationActor;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public record SchematicCaptureContext(Level level, BlockPos position, BlockState state, AutomationActor actor) {
    public SchematicCaptureContext {
        Objects.requireNonNull(level, "level"); Objects.requireNonNull(position, "position");
        Objects.requireNonNull(state, "state"); Objects.requireNonNull(actor, "actor");
    }
}
