package buildcraft.api.v2.automation;

import buildcraft.api.v2.OperationMode;
import buildcraft.api.v2.permission.AutomationActor;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Public context for one Stripes activation. World/location data is immutable; the working item stack can be replaced or consumed. */
public final class StripesContext {
    private final Level level;
    private final BlockPos pipePos;
    private final Direction side;
    private ItemStack stack;
    private final AutomationActor actor;
    private final OperationMode mode;
    private final Player player;
    private final StripesOutput output;

    public StripesContext(Level level, BlockPos pipePos, Direction side, ItemStack stack, AutomationActor actor, OperationMode mode) {
        this(level, pipePos, side, stack, actor, mode, null, StripesOutput.discard());
    }

    public StripesContext(
        Level level,
        BlockPos pipePos,
        Direction side,
        ItemStack stack,
        AutomationActor actor,
        OperationMode mode,
        Player player,
        StripesOutput output
    ) {
        this.level = Objects.requireNonNull(level, "level");
        this.pipePos = Objects.requireNonNull(pipePos, "pipePos").immutable();
        this.side = Objects.requireNonNull(side, "side");
        this.stack = Objects.requireNonNull(stack, "stack").copy();
        this.actor = Objects.requireNonNull(actor, "actor");
        this.mode = Objects.requireNonNull(mode, "mode");
        this.player = player;
        this.output = Objects.requireNonNull(output, "output");
    }

    public Level level() { return level; }
    public BlockPos pipePos() { return pipePos; }
    public Direction side() { return side; }
    /** Snapshot of the current working input stack. */
    public ItemStack stack() { return stack.copy(); }

    /** Replaces the working stack returned to the Stripes runtime after a successful handler. */
    public void replaceStack(ItemStack replacement) {
        this.stack = Objects.requireNonNull(replacement, "replacement").copy();
    }

    /** Consumes up to {@code count} items from the working stack and returns the amount consumed. */
    public int consume(int count) {
        if (count < 0) throw new IllegalArgumentException("count must be non-negative");
        int consumed = Math.min(count, stack.getCount());
        if (consumed > 0) stack.shrink(consumed);
        return consumed;
    }
    public AutomationActor actor() { return actor; }
    public OperationMode mode() { return mode; }
    public Optional<Player> player() { return Optional.ofNullable(player); }
    public StripesOutput output() { return output; }
    public BlockPos targetPos() { return pipePos.relative(side); }
    public boolean hasItem() { return !stack.isEmpty(); }
}
