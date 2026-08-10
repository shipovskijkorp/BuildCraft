package buildcraft.robotics;

import buildcraft.api.transport.pipe.PipeApiClient;
import buildcraft.api.transport.pluggable.IPluggableStaticBaker;
import buildcraft.lib.client.model.ModelHolderStatic;
import buildcraft.robotics.client.model.plug.PlugBakerRobotStation;
import buildcraft.robotics.client.model.key.KeyRobotStation;
import net.neoforged.neoforge.client.event.ModelEvent.BakingCompleted;
import net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional;

public final class BCRoboticsModels {
    public static final ModelHolderStatic ROBOT_STATION_AVAILABLE = new ModelHolderStatic("buildcraftrobotics:pluggables/robot_station");
    public static final ModelHolderStatic ROBOT_STATION_RESERVED = new ModelHolderStatic("buildcraftrobotics:pluggables/robot_station_reserved");
    public static final ModelHolderStatic ROBOT_STATION_LINKED = new ModelHolderStatic("buildcraftrobotics:pluggables/robot_station_linked");
    public static final IPluggableStaticBaker<KeyRobotStation> BAKER_ROBOT_STATION = new PlugBakerRobotStation(
            ROBOT_STATION_AVAILABLE, ROBOT_STATION_RESERVED, ROBOT_STATION_LINKED);

    private BCRoboticsModels() {
    }

    public static void init() {
        PipeApiClient.registry.registerBaker(KeyRobotStation.class, BAKER_ROBOT_STATION);
    }

    /**
     * Robot stations are rendered as pipe pluggables, so their models are not referenced from a normal blockstate.
     * ModernFix's dynamic resource loading can skip such models unless the module explicitly marks them as required.
     */
    public static void onModelBakePre(RegisterAdditional event) {
        event.register(ROBOT_STATION_AVAILABLE.getBakedModelLocation());
        event.register(ROBOT_STATION_RESERVED.getBakedModelLocation());
        event.register(ROBOT_STATION_LINKED.getBakedModelLocation());
    }

    public static void onModelBake(BakingCompleted event) {
        PipeApiClient.registry.registerBaker(KeyRobotStation.class, BAKER_ROBOT_STATION);
    }
}
