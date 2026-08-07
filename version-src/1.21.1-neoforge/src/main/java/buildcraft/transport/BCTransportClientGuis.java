package buildcraft.transport;

import buildcraft.transport.gui.GuiDiamondPipe;
import buildcraft.transport.gui.GuiDiamondWoodPipe;
import buildcraft.transport.gui.GuiEmzuliPipe_BC8;
import buildcraft.transport.gui.GuiFilteredBuffer;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class BCTransportClientGuis {
    private BCTransportClientGuis() {
    }

    public static void clientInit(RegisterMenuScreensEvent event) {
        event.register(BCTransportGuis.MENU_PIPE_DIAMOND_WOOD.get(), GuiDiamondWoodPipe::new);
        event.register(BCTransportGuis.MENU_PIPE_DIAMOND.get(), GuiDiamondPipe::new);
        event.register(BCTransportGuis.MENU_FILTERED_BUFFER.get(), GuiFilteredBuffer::new);
        event.register(BCTransportGuis.MENU_PIPE_EMZULI.get(), GuiEmzuliPipe_BC8::new);
    }
}
