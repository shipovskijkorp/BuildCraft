package buildcraft.api.v2.robot;

import buildcraft.api.v2.OperationMode;
import net.minecraft.core.BlockPos;

public interface RobotControl {
    boolean moveTo(BlockPos target, OperationMode mode);
    boolean assign(RobotTask task, OperationMode mode);
    boolean cancelTask(OperationMode mode);
}
