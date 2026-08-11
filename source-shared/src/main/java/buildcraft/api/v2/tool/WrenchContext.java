package buildcraft.api.v2.tool;

import buildcraft.api.v2.OperationMode;
import buildcraft.api.v2.permission.AutomationActor;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public record WrenchContext(Level level, BlockPos pos, Direction face, ItemStack tool, AutomationActor actor, OperationMode mode) {
    public WrenchContext {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(face, "face");
        Objects.requireNonNull(tool, "tool");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(mode, "mode");
    }
}
