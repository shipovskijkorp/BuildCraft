package buildcraft.transport.api2;

import buildcraft.api.v2.automation.AutomationResult;
import buildcraft.api.v2.automation.StripesContext;
import buildcraft.api.v2.automation.StripesHandler;
import buildcraft.api.v2.automation.StripesOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Internal adapters used while built-in BC8 Stripes logic is executed through the public API2 registry. */
public final class StripesApi2Bridge {
    @FunctionalInterface
    public interface ItemHandler {
        boolean handle(Level level, BlockPos pipePos, Direction side, ItemStack stack, Player player, StripesOutput output);
    }

    @FunctionalInterface
    public interface BlockHandler {
        boolean handle(Level level, BlockPos pipePos, Direction side, Player player, StripesOutput output);
    }

    private StripesApi2Bridge() {}

    public static StripesHandler item(ItemHandler handler) { return item(handler, 0); }

    public static StripesHandler item(ItemHandler handler, int priority) {
        return new StripesHandler() {
            @Override public AutomationResult activate(StripesContext context) {
                if (!context.hasItem()) return AutomationResult.pass();
                Player player = context.player().orElse(null);
                if (player == null) return AutomationResult.pass();
                ItemStack working = context.stack();
                player.getInventory().setItem(player.getInventory().selected, working);
                boolean handled = handler.handle(
                    context.level(), context.pipePos(), context.side(), working, player, context.output()
                );
                if (!handled) return AutomationResult.pass();
                context.replaceStack(player.getInventory().getItem(player.getInventory().selected));
                return AutomationResult.success(1);
            }
            @Override public int priority() { return priority; }
        };
    }

    public static StripesHandler block(BlockHandler handler) { return block(handler, 0); }

    public static StripesHandler block(BlockHandler handler, int priority) {
        return new StripesHandler() {
            @Override public AutomationResult activate(StripesContext context) {
                if (context.hasItem()) return AutomationResult.pass();
                Player player = context.player().orElse(null);
                if (player == null) return AutomationResult.pass();
                return handler.handle(context.level(), context.pipePos(), context.side(), player, context.output())
                    ? AutomationResult.success(1)
                    : AutomationResult.pass();
            }
            @Override public int priority() { return priority; }
        };
    }
}
