package buildcraft.api.v2.context;

import buildcraft.api.v2.area.AreaProvider;
import buildcraft.api.v2.energy.MjPort;
import buildcraft.api.v2.fluid.FluidPort;
import buildcraft.api.v2.gate.GateView;
import buildcraft.api.v2.item.ItemPort;
import buildcraft.api.v2.machine.MachineView;
import buildcraft.api.v2.pipe.PipeView;
import buildcraft.api.v2.platform.ExternalEnergyPort;
import buildcraft.api.v2.robot.RobotHandle;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public final class BuildCraftContextKeys {
    public static final ContextKey<PipeView> PIPE = key("pipe", PipeView.class);
    public static final ContextKey<GateView> GATE = key("gate", GateView.class);
    public static final ContextKey<RobotHandle> ROBOT = key("robot", RobotHandle.class);
    public static final ContextKey<MachineView> MACHINE = key("machine", MachineView.class);
    public static final ContextKey<AreaProvider> AREA = key("area", AreaProvider.class);
    public static final ContextKey<ItemPort> ITEM_PORT = key("item_port", ItemPort.class);
    public static final ContextKey<FluidPort> FLUID_PORT = key("fluid_port", FluidPort.class);
    public static final ContextKey<MjPort> MJ_PORT = key("mj_port", MjPort.class);
    public static final ContextKey<ExternalEnergyPort> EXTERNAL_ENERGY_PORT = key("external_energy_port", ExternalEnergyPort.class);

    private BuildCraftContextKeys() {}

    private static <T> ContextKey<T> key(String path, Class<T> type) {
        return ContextKey.of(Objects.requireNonNull(ResourceLocation.tryParse("buildcraft:" + path)), type);
    }
}
