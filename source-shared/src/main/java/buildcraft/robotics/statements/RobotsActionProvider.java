package buildcraft.robotics.statements;

import java.util.Collection;

import javax.annotation.Nonnull;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import buildcraft.lib.internal.statement.IActionExternal;
import buildcraft.lib.internal.statement.IActionInternal;
import buildcraft.lib.internal.statement.IActionInternalSided;
import buildcraft.lib.internal.statement.IActionProvider;
import buildcraft.lib.internal.statement.IStatementContainer;
import buildcraft.transport.internal.pipe.IPipeHolder;
import buildcraft.robotics.BCRoboticsStatements;
import buildcraft.robotics.plug.RobotStationPluggable;

public class RobotsActionProvider implements IActionProvider {

    @Override
    public void addInternalActions(Collection<IActionInternal> actions, IStatementContainer container) {
        if (!(container.getTile() instanceof IPipeHolder holder)) return;

        boolean hasStation = false;
        for (Direction side : Direction.values()) {
            if (holder.getPluggable(side) instanceof RobotStationPluggable) {
                hasStation = true;
                break;
            }
        }
        if (!hasStation) return;

        actions.add(BCRoboticsStatements.ACTION_ROBOT_FILTER);
        actions.add(BCRoboticsStatements.ACTION_ROBOT_FILTER_TOOL);
        actions.add(BCRoboticsStatements.ACTION_ROBOT_GOTO_STATION);
        actions.add(BCRoboticsStatements.ACTION_ROBOT_WAKEUP);
        actions.add(BCRoboticsStatements.ACTION_ROBOT_WORK_IN_AREA);
        actions.add(BCRoboticsStatements.ACTION_ROBOT_LOAD_UNLOAD_AREA);
        actions.add(BCRoboticsStatements.ACTION_STATION_ACCEPT_ITEMS);
        actions.add(BCRoboticsStatements.ACTION_STATION_ACCEPT_FLUIDS);
        actions.add(BCRoboticsStatements.ACTION_STATION_FORBID_ROBOT);
        actions.add(BCRoboticsStatements.ACTION_STATION_FORCE_ROBOT);
        actions.add(BCRoboticsStatements.ACTION_STATION_PROVIDE_ITEMS);
        actions.add(BCRoboticsStatements.ACTION_STATION_PROVIDE_FLUIDS);
        actions.add(BCRoboticsStatements.ACTION_STATION_REQUEST_ITEMS);
        actions.add(BCRoboticsStatements.ACTION_STATION_MACHINE_REQUEST);
    }

    @Override
    public void addInternalSidedActions(Collection<IActionInternalSided> actions,
            IStatementContainer container, @Nonnull Direction side) {
        // No sided-only actions for robots
    }

    @Override
    public void addExternalActions(Collection<IActionExternal> actions,
            @Nonnull Direction side, BlockEntity tile) {
        // No external actions
    }
}
