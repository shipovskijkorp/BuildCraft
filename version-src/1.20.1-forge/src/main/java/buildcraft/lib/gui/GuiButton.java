package buildcraft.lib.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class GuiButton {
    private final int posX;
    private final int posY;
    private int state;
    private final int statesNum;
    private final boolean shouldRightReduce;
    private boolean active = true;
    private ResourceLocation defaultBg;
    private ResourceLocation holdingBg;

    public GuiButton(int x, int y, boolean rightReduce, int states) {
        posX = x;
        posY = y;
        statesNum = Math.max(1, states);
        shouldRightReduce = rightReduce;
    }

    public void render(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
    }

    protected boolean isHolding(double x, double y) {
        return false;
    }

    public void onMouseClick(double x, double y, int type) {
        if (!active || !isHolding(x, y)) {
            return;
        }
        if (type == 1 && shouldRightReduce) {
            state = Math.floorMod(state - 1, statesNum);
        } else {
            state = (state + 1) % statesNum;
        }
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public void setState(int state) { this.state = Math.floorMod(state, statesNum); }
    public int getState() { return state; }
}
