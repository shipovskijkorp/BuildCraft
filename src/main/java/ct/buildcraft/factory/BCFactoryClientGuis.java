package ct.buildcraft.factory;

import ct.buildcraft.factory.client.gui.MenuHeatExchange;
import ct.buildcraft.factory.client.gui.ScreenHeatExchange;
import ct.buildcraft.factory.container.ContainerAutoCraftItems;
import ct.buildcraft.factory.container.ContainerChute;
import ct.buildcraft.factory.container.ContainerTank;
import ct.buildcraft.factory.gui.GuiAutoCraftItems;
import ct.buildcraft.factory.gui.GuiChute;
import ct.buildcraft.factory.gui.GuiTank;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class BCFactoryClientGuis {
    private BCFactoryClientGuis() {
    }

    public static void clientInit(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(BCFactoryGuis.MENU_AUTOWORK_BENCH_ITEM.get(), GuiAutoCraftItems::new);
            MenuScreens.register(BCFactoryGuis.MENU_HEAT_EXCHANGE.get(), ScreenHeatExchange::new);
            MenuScreens.register(BCFactoryGuis.MENU_CHUTE.get(), GuiChute::new);
            MenuScreens.register(BCFactoryGuis.MENU_TANK.get(), GuiTank::new);
        });
    }
}
