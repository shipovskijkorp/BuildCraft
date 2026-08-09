package buildcraft.lib.gui;

import buildcraft.lib.gui.json.BuildCraftJsonGui;
import buildcraft.lib.gui.ledger.LedgerHelp;
import buildcraft.lib.gui.pos.IGuiArea;
import buildcraft.lib.misc.GuiUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/** Reference screen that delegates its element handling to {@link BuildCraftGui}. */
public class GuiScreenBuildCraft extends Screen {
    public final BuildCraftGui mainGui;

    public GuiScreenBuildCraft(Component title) {
        this(gui -> new BuildCraftGui(gui), title);
    }

    public GuiScreenBuildCraft(IGuiArea area, Component title) {
        this(gui -> new BuildCraftGui(gui, area), title);
    }

    public GuiScreenBuildCraft(Function<GuiScreenBuildCraft, BuildCraftGui> constructor, Component title) {
        super(title);
        mainGui = constructor.apply(this);
        standardLedgerInit();
    }

    public GuiScreenBuildCraft(ResourceLocation jsonGuiDef, Component title) {
        super(title);
        mainGui = new BuildCraftJsonGui(this, jsonGuiDef);
        standardLedgerInit();
    }

    public GuiScreenBuildCraft(ResourceLocation jsonGuiDef, IGuiArea area, Component title) {
        super(title);
        mainGui = new BuildCraftJsonGui(this, area, jsonGuiDef);
        standardLedgerInit();
    }

    private void standardLedgerInit() {
        if (shouldAddHelpLedger()) {
            mainGui.shownElements.add(new LedgerHelp(mainGui, false));
        }
    }

    protected boolean shouldAddHelpLedger() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        mainGui.tick();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        mainGui.drawBackgroundLayer(guiGraphics, partialTicks, mouseX, mouseY,
            () -> drawMenuBackground(guiGraphics, mouseX, mouseY, partialTicks));
        mainGui.drawElementBackgrounds(guiGraphics);
        mainGui.drawElementForegrounds(() -> drawMenuBackground(guiGraphics, mouseX, mouseY, partialTicks), guiGraphics);
    }

    private void drawMenuBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        return mainGui.onMouseClicked(mouseX, mouseY, mouseButton)
            || super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean result = super.mouseReleased(mouseX, mouseY, button);
        mainGui.onMouseReleased(mouseX, mouseY, button);
        return result;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        boolean result = super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        mainGui.onMouseDragged(mouseX, mouseY, button, dragX, dragY);
        return result;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!mainGui.onKeyTyped(modifiers, InputConstants.getKey(keyCode, scanCode))) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        return true;
    }
}
