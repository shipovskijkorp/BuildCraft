package buildcraft.factory;

import buildcraft.factory.client.gui.MenuHeatExchange;
import buildcraft.factory.client.gui.ScreenHeatExchange;
import buildcraft.factory.container.ContainerAutoCraftItems;
import buildcraft.factory.container.ContainerChute;
import buildcraft.factory.container.ContainerTank;
import buildcraft.factory.gui.GuiAutoCraftItems;
import buildcraft.factory.gui.GuiChute;
import buildcraft.factory.gui.GuiTank;
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
