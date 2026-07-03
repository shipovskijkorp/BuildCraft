package ct.buildcraft.transport;

import ct.buildcraft.transport.gui.GuiDiamondPipe;
import ct.buildcraft.transport.gui.GuiDiamondWoodPipe;
import ct.buildcraft.transport.gui.GuiEmzuliPipe_BC8;
import ct.buildcraft.transport.gui.GuiFilteredBuffer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.fml.event.lifecycle.ParallelDispatchEvent;

public final class BCTransportClientGuis {
    private BCTransportClientGuis() {
    }

    public static void clientInit(ParallelDispatchEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(BCTransportGuis.MENU_PIPE_DIAMOND_WOOD.get(), GuiDiamondWoodPipe::new);
            MenuScreens.register(BCTransportGuis.MENU_PIPE_DIAMOND.get(), GuiDiamondPipe::new);
            MenuScreens.register(BCTransportGuis.MENU_FILTERED_BUFFER.get(), GuiFilteredBuffer::new);
            MenuScreens.register(BCTransportGuis.MENU_PIPE_EMZULI.get(), GuiEmzuliPipe_BC8::new);
        });
    }
}
