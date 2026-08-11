package buildcraft.api.v2.robot;

import java.util.Collection;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public interface RobotService {
    Optional<RobotHandle> robot(Level level, long id);
    Collection<? extends RobotHandle> robots(Level level);
    Optional<RobotDock> dock(Level level, BlockPos pos, Direction side);
    Optional<RobotResourceLease> acquire(Level level, long robotId, RobotResourceRequest request);

    /**
     * Evaluates registered robot lifecycle listeners. Implementations must use deterministic
     * listener ordering and {@link RobotEventDecision#merge(RobotEventDecision)} semantics.
     */
    RobotEventDecision evaluateEvent(RobotEventContext context);
}
