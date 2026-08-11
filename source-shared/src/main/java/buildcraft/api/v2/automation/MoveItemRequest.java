package buildcraft.api.v2.automation;

import buildcraft.api.v2.OperationMode;
import buildcraft.api.v2.item.ItemMatcher;
import buildcraft.api.v2.permission.AutomationActor;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public record MoveItemRequest(Level level, BlockPos origin, BlockPos target, ItemMatcher matcher, int maxCount, AutomationActor actor, OperationMode mode) implements AutomationRequest {
    public MoveItemRequest {
        Objects.requireNonNull(level, "level"); Objects.requireNonNull(origin, "origin"); Objects.requireNonNull(target, "target");
        Objects.requireNonNull(matcher, "matcher"); Objects.requireNonNull(actor, "actor"); Objects.requireNonNull(mode, "mode");
        if (maxCount < 0) throw new IllegalArgumentException("maxCount must be non-negative");
    }
    @Override public ResourceLocation kind() { return AutomationKinds.MOVE_ITEM; }
}
