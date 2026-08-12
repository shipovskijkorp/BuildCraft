package buildcraft.api.v2.schematic;

import buildcraft.api.v2.permission.AutomationActor;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** Entity capture context for blueprint/schematic adapters. */
public record SchematicEntityCaptureContext(Level level, BlockPos origin, Entity entity, AutomationActor actor) {
    public SchematicEntityCaptureContext {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(actor, "actor");
    }
}
