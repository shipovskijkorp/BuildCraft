package buildcraft.api.v2.world;

import buildcraft.api.v2.permission.AutomationActor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Centralized world/block rules used by builders, robots, quarry and transport automation. */
public interface WorldRuleService {
    boolean isSoft(Level level, BlockPos pos, BlockState state);
    boolean isReplaceable(Level level, BlockPos pos, BlockState state);
    boolean mayBreak(Level level, BlockPos pos, BlockState state, AutomationActor actor);
    boolean mayPlace(Level level, BlockPos pos, BlockState state, AutomationActor actor);
}
