package buildcraft.robotics.statements;

import java.util.List;

import buildcraft.api.items.IMapLocation;
import buildcraft.api.robots.DockingStation;
import buildcraft.api.robots.EntityRobotBase;
import buildcraft.api.robots.IRobotRegistry;
import buildcraft.api.robots.RobotManager;
import buildcraft.api.statements.IActionInternal;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.core.statements.BCStatement;
import buildcraft.api.core.render.ISprite;
import buildcraft.robotics.BCRoboticsSprites;
import buildcraft.robotics.ai.AIRobotGoAndLinkToDock;
import buildcraft.robotics.plug.RobotStationPluggable;
import buildcraft.transport.internal.pipe.IPipeHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ActionRobotGotoStation extends BCStatement implements IActionInternal {

    public ActionRobotGotoStation() {
        super("buildcraft:robot.goto_station");
    }

    @Override
    public int maxParameters() {
        return 1;
    }

    @Override
    public IStatementParameter createParameter(int index) {
        return new StatementParameterMapLocation();
    }

    @Override
    public Component getDescription() {
        return Component.translatable("gate.action.robot.gotoStation");
    }

    @Override
    public void actionActivate(IStatementContainer container, IStatementParameter[] parameters) {
        if (!(container.getTile() instanceof IPipeHolder holder) || RobotManager.registryProvider == null) {
            return;
        }

        IRobotRegistry registry = RobotManager.registryProvider.getRegistry(holder.getPipeWorld());
        DockingStation requestedStation = getRequestedStation(parameters, registry);

        for (DockingStation station : getStations(holder)) {
            EntityRobotBase robot = station.robotTaking();
            if (robot == null) {
                continue;
            }
            DockingStation target = requestedStation == null ? station : requestedStation;
            if (target != null) {
                robot.setMainAIOverride(new AIRobotGoAndLinkToDock(robot, target));
            }
        }
    }

    private static List<DockingStation> getStations(IPipeHolder holder) {
        java.util.ArrayList<DockingStation> stations = new java.util.ArrayList<>();
        for (Direction side : Direction.values()) {
            if (holder.getPluggable(side) instanceof RobotStationPluggable plug) {
                DockingStation station = plug.getStation();
                if (station != null) {
                    stations.add(station);
                }
            }
        }
        return stations;
    }

    private static DockingStation getRequestedStation(IStatementParameter[] parameters, IRobotRegistry registry) {
        if (parameters == null || parameters.length == 0 || !(parameters[0] instanceof StatementParameterMapLocation param)) {
            return null;
        }
        ItemStack stack = param.getItemStack();
        if (stack.isEmpty() || !(stack.getItem() instanceof IMapLocation map)) {
            return null;
        }
        BlockPos pos = map.getPoint(stack);
        if (pos == null) {
            return null;
        }
        Direction side = map.getPointSide(stack);
        return registry.getStation(pos, side);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ISprite getSprite() {
        return BCRoboticsSprites.ACTION_ROBOT_GOTO_STATION;
    }
}
