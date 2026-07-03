package ct.buildcraft.robotics;

import ct.buildcraft.robotics.gui.GuiRequester;
import ct.buildcraft.robotics.gui.GuiZonePlanner;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class BCRoboticsClientGuis {
    private BCRoboticsClientGuis() {
    }

    public static void clientInit(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(BCRoboticsGuis.MENU_ZONE_PLANNER.get(), GuiZonePlanner::new);
            MenuScreens.register(BCRoboticsGuis.MENU_REQUESTER.get(), GuiRequester::new);
        });
    }
}
