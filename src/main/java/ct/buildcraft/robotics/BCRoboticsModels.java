package ct.buildcraft.robotics;

import ct.buildcraft.api.transport.pipe.PipeApiClient;
import ct.buildcraft.api.transport.pluggable.IPluggableStaticBaker;
import ct.buildcraft.lib.client.model.ModelHolderStatic;
import ct.buildcraft.robotics.client.model.plug.PlugBakerRobotStation;
import ct.buildcraft.robotics.client.model.key.KeyRobotStation;
import net.minecraftforge.client.event.ModelEvent.BakingCompleted;

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

    public static void onModelBake(BakingCompleted event) {
        PipeApiClient.registry.registerBaker(KeyRobotStation.class, BAKER_ROBOT_STATION);
    }
}
