package buildcraft.api.v2.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface WorldProperty {
    boolean test(Level level, BlockPos pos);
    default void clear() {}
}
