package ct.buildcraft.silicon.gui;

import com.mojang.blaze3d.vertex.PoseStack;

import ct.buildcraft.lib.gui.GuiBC8;
import ct.buildcraft.lib.gui.GuiIcon;
import ct.buildcraft.lib.gui.pos.GuiRectangle;
import ct.buildcraft.silicon.container.ContainerProgrammingTable;
import ct.buildcraft.silicon.tile.TileProgrammingTable_Neptune;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GuiProgrammingTable extends GuiBC8<ContainerProgrammingTable> {
    private static final ResourceLocation TEXTURE_BASE = new ResourceLocation("buildcraftsilicon:textures/gui/programming_table.png");
    private static final int SIZE_X = 176;
    private static final int SIZE_Y = 207;
    private static final GuiIcon ICON_GUI = new GuiIcon(TEXTURE_BASE, 0, 0, SIZE_X, SIZE_Y);
    private static final GuiIcon ICON_PROGRESS = new GuiIcon(TEXTURE_BASE, 176, 18, 4, 70);
    private static final GuiIcon ICON_SELECTED = new GuiIcon(TEXTURE_BASE, 196, 1, 16, 16);
    private static final GuiRectangle RECT_PROGRESS = new GuiRectangle(164, 36, 4, 70);

    public GuiProgrammingTable(ContainerProgrammingTable container, Inventory inv, Component title) {
        super(container, inv, title);
        imageWidth = SIZE_X;
        imageHeight = SIZE_Y;
        mainGui.shownElements.add(new LedgerTablePower(mainGui, container.tile, true));
    }

    private int optionX(int index) {
        return 43 + (index % TileProgrammingTable_Neptune.WIDTH) * 18;
    }

    private int optionY(int index) {
        return 36 + (index / TileProgrammingTable_Neptune.WIDTH) * 18;
    }

    @Override
    protected void drawBackgroundLayer(PoseStack pose, int mouseX, int mouseY, float partialTicks) {
        ICON_GUI.drawAt(pose, mainGui.rootElement);

        long target = container.tile.getGuiTarget();
        if (target != 0) {
            double v = (double) container.tile.power / target;
            ICON_PROGRESS.drawCutInside(
                    pose, new GuiRectangle(
                            RECT_PROGRESS.x,
                            (int) (RECT_PROGRESS.y + RECT_PROGRESS.height * Math.max(1 - v, 0)),
                            RECT_PROGRESS.width,
                            (int) Math.ceil(RECT_PROGRESS.height * Math.min(v, 1))
                    ).offset(mainGui.rootElement)
            );
        }

        int selected = container.tile.selectedOption;
        if (selected >= 0 && selected < TileProgrammingTable_Neptune.OPTION_COUNT) {
            ICON_SELECTED.drawAt(pose, new GuiRectangle(optionX(selected), optionY(selected), 16, 16).offset(mainGui.rootElement));
        }
    }

    @Override
    protected void drawForegroundLayer(PoseStack pose, int mouseX, int mouseY) {
        font.draw(pose, title, leftPos + (imageWidth - font.width(title)) / 2, topPos + 15, 0x404040);
        font.draw(pose, playerInventoryTitle, leftPos + 8, topPos + imageHeight - 97, 0x404040);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        boolean flag = super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0) {
            for (int i = 0; i < TileProgrammingTable_Neptune.OPTION_COUNT; i++) {
                int x = leftPos + optionX(i);
                int y = topPos + optionY(i);
                if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                    container.sendSelectOption(container.tile.selectedOption == i ? -1 : i);
                    return true;
                }
            }
        }
        return flag;
    }
}
