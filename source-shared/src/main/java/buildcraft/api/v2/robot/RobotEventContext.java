package buildcraft.api.v2.robot;

import buildcraft.api.v2.permission.AutomationActor;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public record RobotEventContext(
    RobotEventKind kind,
    RobotHandle robot,
    Level level,
    Optional<AutomationActor> actor,
    ItemStack heldItem
) {
    public RobotEventContext {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(robot, "robot");
        Objects.requireNonNull(level, "level");
        actor = Objects.requireNonNull(actor, "actor");
        heldItem = Objects.requireNonNull(heldItem, "heldItem").copy();
    }

    @Override public ItemStack heldItem() { return heldItem.copy(); }
}
