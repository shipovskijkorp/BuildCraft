package buildcraft.api.v2.automation;

import buildcraft.api.v2.OperationMode;
import buildcraft.api.v2.fluid.FluidAmount;
import buildcraft.api.v2.fluid.FluidMatcher;
import buildcraft.api.v2.permission.AutomationActor;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public record MoveFluidRequest(Level level, BlockPos origin, BlockPos target, FluidMatcher matcher, FluidAmount maxAmount, AutomationActor actor, OperationMode mode) implements AutomationRequest {
    public MoveFluidRequest {
        Objects.requireNonNull(level, "level"); Objects.requireNonNull(origin, "origin"); Objects.requireNonNull(target, "target");
        Objects.requireNonNull(matcher, "matcher"); Objects.requireNonNull(maxAmount, "maxAmount");
        Objects.requireNonNull(actor, "actor"); Objects.requireNonNull(mode, "mode");
    }
    @Override public ResourceLocation kind() { return AutomationKinds.MOVE_FLUID; }
}
