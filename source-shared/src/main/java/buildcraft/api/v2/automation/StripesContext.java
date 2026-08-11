package buildcraft.api.v2.automation;

import buildcraft.api.v2.OperationMode;
import buildcraft.api.v2.permission.AutomationActor;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public record StripesContext(Level level, BlockPos pipePos, Direction side, ItemStack stack, AutomationActor actor, OperationMode mode) {
    public StripesContext {
        Objects.requireNonNull(level, "level"); Objects.requireNonNull(pipePos, "pipePos"); Objects.requireNonNull(side, "side");
        stack = Objects.requireNonNull(stack, "stack").copy(); Objects.requireNonNull(actor, "actor"); Objects.requireNonNull(mode, "mode");
    }
    @Override public ItemStack stack() { return stack.copy(); }
}
