package buildcraft.robotics;

import buildcraft.lib.gui.BCContainerFactory;
import buildcraft.robotics.container.ContainerRequester;
import buildcraft.robotics.container.ContainerZonePlanner;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class BCRoboticsGuis {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, BCRobotics.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ContainerZonePlanner>> MENU_ZONE_PLANNER = MENUS.register(
            "menu.zone_planner",
            () -> BCContainerFactory.create(ContainerZonePlanner::new)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<ContainerRequester>> MENU_REQUESTER = MENUS.register(
            "menu.requester",
            () -> BCContainerFactory.create(ContainerRequester::new)
    );

    private BCRoboticsGuis() {
    }


    public static void registry(IEventBus bus) {
        MENUS.register(bus);
    }
}
