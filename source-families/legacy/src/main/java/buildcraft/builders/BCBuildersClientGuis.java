package buildcraft.builders;

import buildcraft.builders.gui.GuiArchitectTable;
import buildcraft.builders.gui.GuiBuilder;
import buildcraft.builders.gui.GuiElectronicLibrary;
import buildcraft.builders.gui.GuiFiller;
import buildcraft.builders.gui.ScreenReplacer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class BCBuildersClientGuis {
    private BCBuildersClientGuis() {
    }

    public static void clientInit(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(BCBuildersGuis.MENU_ARCHITECT_TABLE.get(), GuiArchitectTable::new);
            MenuScreens.register(BCBuildersGuis.MENU_BUILDER.get(), GuiBuilder::new);
            MenuScreens.register(BCBuildersGuis.MENU_ELIBRARY.get(), GuiElectronicLibrary::new);
            MenuScreens.register(BCBuildersGuis.MENU_FILLER.get(), GuiFiller::new);
            MenuScreens.register(BCBuildersGuis.MENU_REPLACER.get(), ScreenReplacer::new);
        });
    }
}
