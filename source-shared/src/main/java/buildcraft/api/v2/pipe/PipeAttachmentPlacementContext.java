package buildcraft.api.v2.pipe;

import buildcraft.api.v2.OperationMode;
import buildcraft.api.v2.permission.AutomationActor;
import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public record PipeAttachmentPlacementContext(PipeView pipe, Direction side, ItemStack stack, AutomationActor actor, OperationMode mode) {
    public PipeAttachmentPlacementContext {
        Objects.requireNonNull(pipe, "pipe"); Objects.requireNonNull(side, "side"); stack = Objects.requireNonNull(stack, "stack").copy();
        Objects.requireNonNull(actor, "actor"); Objects.requireNonNull(mode, "mode");
    }
    @Override public ItemStack stack() { return stack.copy(); }
}
