package buildcraft.api.v2.automation;

import buildcraft.api.v2.OperationMode;
import buildcraft.api.v2.permission.AutomationActor;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public record BreakBlockRequest(Level level, BlockPos origin, BlockPos target, AutomationActor actor, OperationMode mode) implements AutomationRequest {
    public BreakBlockRequest {
        Objects.requireNonNull(level, "level"); Objects.requireNonNull(origin, "origin"); Objects.requireNonNull(target, "target");
        Objects.requireNonNull(actor, "actor"); Objects.requireNonNull(mode, "mode");
    }
    @Override public ResourceLocation kind() { return AutomationKinds.BREAK_BLOCK; }
}
