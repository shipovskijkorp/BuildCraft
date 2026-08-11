package buildcraft.api.v2.automation;

import buildcraft.api.v2.OperationMode;
import buildcraft.api.v2.permission.AutomationActor;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public record UseItemRequest(Level level, BlockPos origin, BlockPos target, Direction side, ItemStack stack, AutomationActor actor, OperationMode mode) implements AutomationRequest {
    public UseItemRequest {
        Objects.requireNonNull(level, "level"); Objects.requireNonNull(origin, "origin"); Objects.requireNonNull(target, "target");
        Objects.requireNonNull(side, "side"); stack = Objects.requireNonNull(stack, "stack").copy();
        Objects.requireNonNull(actor, "actor"); Objects.requireNonNull(mode, "mode");
    }
    @Override public ItemStack stack() { return stack.copy(); }
    @Override public ResourceLocation kind() { return AutomationKinds.USE_ITEM; }
}
