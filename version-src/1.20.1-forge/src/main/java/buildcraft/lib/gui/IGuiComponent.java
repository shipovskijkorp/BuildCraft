package buildcraft.lib.gui;

import net.minecraft.client.gui.GuiGraphics;

public abstract class IGuiComponent {
    public int posX;
    public int posY;
    public boolean active = true;

    public abstract void render(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY);
    public abstract void onMouseClick(double x, double y, int type);

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
