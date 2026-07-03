package ct.buildcraft.silicon;

import ct.buildcraft.silicon.gui.GuiAdvancedCraftingTable;
import ct.buildcraft.silicon.gui.GuiAssemblyTable;
import ct.buildcraft.silicon.gui.GuiChargingTable;
import ct.buildcraft.silicon.gui.GuiGate;
import ct.buildcraft.silicon.gui.GuiIntegrationTable;
import ct.buildcraft.silicon.gui.GuiProgrammingTable;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.fml.event.lifecycle.ParallelDispatchEvent;

public final class BCSiliconClientGuis {
    private BCSiliconClientGuis() {
    }

    public static void clientInit(ParallelDispatchEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(BCSiliconGuis.MENU_AD_CRAFTING_TABLE.get(), GuiAdvancedCraftingTable::new);
            MenuScreens.register(BCSiliconGuis.MENU_ASSEMBLY_TABLE.get(), GuiAssemblyTable::new);
            MenuScreens.register(BCSiliconGuis.MENU_CHARGING_TABLE.get(), GuiChargingTable::new);
            MenuScreens.register(BCSiliconGuis.MENU_GATE.get(), GuiGate::new);
            MenuScreens.register(BCSiliconGuis.MENU_INTEGRATION_TABLE.get(), GuiIntegrationTable::new);
            MenuScreens.register(BCSiliconGuis.MENU_PROGRAMMING_TABLE.get(), GuiProgrammingTable::new);
        });
    }
}
