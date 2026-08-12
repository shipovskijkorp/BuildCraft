package buildcraft.api.v2.schematic;

import buildcraft.api.v2.OperationMode;
import buildcraft.api.v2.permission.AutomationActor;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Context used to simulate or execute placement of a captured element. */
public record SchematicPlacementContext(
    Level level,
    BlockPos origin,
    BlockPos position,
    AutomationActor actor,
    OperationMode mode
) {
    public SchematicPlacementContext {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(mode, "mode");
    }

    /** Convenience constructor for block-local placement. */
    public SchematicPlacementContext(Level level, BlockPos position, AutomationActor actor, OperationMode mode) {
        this(level, position, position, actor, mode);
    }
}
