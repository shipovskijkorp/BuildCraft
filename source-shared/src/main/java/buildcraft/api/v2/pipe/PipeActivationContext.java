package buildcraft.api.v2.pipe;

import buildcraft.api.v2.OperationMode;
import buildcraft.api.v2.permission.AutomationActor;
import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public record PipeActivationContext(PipeMutationContext pipe, Direction side, ItemStack heldStack, AutomationActor actor, OperationMode mode) {
    public PipeActivationContext {
        Objects.requireNonNull(pipe, "pipe"); Objects.requireNonNull(side, "side");
        heldStack = Objects.requireNonNull(heldStack, "heldStack").copy();
        Objects.requireNonNull(actor, "actor"); Objects.requireNonNull(mode, "mode");
    }
    @Override public ItemStack heldStack() { return heldStack.copy(); }
}
