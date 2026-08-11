package buildcraft.api.v2.schematic;

import buildcraft.api.v2.OperationMode;
import buildcraft.api.v2.permission.AutomationActor;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public record SchematicPlacementContext(Level level, BlockPos position, AutomationActor actor, OperationMode mode) {
    public SchematicPlacementContext {
        Objects.requireNonNull(level, "level"); Objects.requireNonNull(position, "position");
        Objects.requireNonNull(actor, "actor"); Objects.requireNonNull(mode, "mode");
    }
}
