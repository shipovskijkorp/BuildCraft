package buildcraft.factory;

import buildcraft.factory.client.gui.MenuHeatExchange;
import buildcraft.factory.container.ContainerAutoCraftItems;
import buildcraft.factory.container.ContainerChute;
import buildcraft.factory.container.ContainerTank;
import buildcraft.lib.gui.BCContainerFactory;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BCFactoryGuis {
	
	public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, BCFactory.MODID);
	
    public static final RegistryObject<MenuType<ContainerAutoCraftItems>> MENU_AUTOWORK_BENCH_ITEM = MENUS.register("menu.autoworkbench_item", () -> BCContainerFactory.create(ContainerAutoCraftItems::create));
    public static final RegistryObject<MenuType<MenuHeatExchange>> MENU_HEAT_EXCHANGE = MENUS.register("menu.heat_exchange", () -> BCContainerFactory.create(MenuHeatExchange::new));
    public static final RegistryObject<MenuType<ContainerChute>> MENU_CHUTE = MENUS.register("menu.chute", () -> BCContainerFactory.create(ContainerChute::new));
    public static final RegistryObject<MenuType<ContainerTank>> MENU_TANK = MENUS.register("menu.tank", () -> BCContainerFactory.create(ContainerTank::new));
    static void registry(IEventBus bus) {
        MENUS.register(bus);
    }
}
