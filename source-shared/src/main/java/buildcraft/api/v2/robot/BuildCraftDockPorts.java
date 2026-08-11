package buildcraft.api.v2.robot;

import buildcraft.api.v2.energy.MjPort;
import buildcraft.api.v2.fluid.FluidPort;
import buildcraft.api.v2.item.ItemPort;
import buildcraft.api.v2.platform.ExternalEnergyPort;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public final class BuildCraftDockPorts {
    public static final DockPortType<ItemPort> ITEMS = port("items", ItemPort.class);
    public static final DockPortType<FluidPort> FLUIDS = port("fluids", FluidPort.class);
    public static final DockPortType<MjPort> MJ = port("mj", MjPort.class);
    public static final DockPortType<ExternalEnergyPort> EXTERNAL_ENERGY = port("external_energy", ExternalEnergyPort.class);

    private BuildCraftDockPorts() {}
    private static <T> DockPortType<T> port(String path, Class<T> type) {
        return new DockPortType<>(Objects.requireNonNull(ResourceLocation.tryParse("buildcraft:" + path)), type);
    }
}
