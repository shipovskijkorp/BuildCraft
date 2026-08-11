package buildcraft.api.v2.automation;

import buildcraft.api.v2.OperationMode;
import buildcraft.api.v2.permission.AutomationActor;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public record PlaceBlockRequest(Level level, BlockPos origin, BlockPos target, BlockState state, AutomationActor actor, OperationMode mode) implements AutomationRequest {
    public PlaceBlockRequest {
        Objects.requireNonNull(level, "level"); Objects.requireNonNull(origin, "origin"); Objects.requireNonNull(target, "target");
        Objects.requireNonNull(state, "state"); Objects.requireNonNull(actor, "actor"); Objects.requireNonNull(mode, "mode");
    }
    @Override public ResourceLocation kind() { return AutomationKinds.PLACE_BLOCK; }
}
