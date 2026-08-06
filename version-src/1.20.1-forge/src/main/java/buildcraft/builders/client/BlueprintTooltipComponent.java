package buildcraft.builders.client;

import buildcraft.api.schematics.ISchematicBlock;
import buildcraft.builders.BCBuildersItems;
import buildcraft.builders.item.ItemSchematicSingle;
import buildcraft.builders.item.ItemSnapshot;
import buildcraft.builders.snapshot.Blueprint;
import buildcraft.builders.snapshot.ClientSnapshots;
import buildcraft.builders.snapshot.Snapshot;
import buildcraft.builders.snapshot.Snapshot.Header;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

public class BlueprintTooltipComponent implements ClientTooltipComponent {
    private static final int PREVIEW_SIZE = 100;

    private final ItemStack blueprint;

    public BlueprintTooltipComponent(BlueprintTooltip tooltip) {
        this.blueprint = tooltip.getBlueprint();
    }

    @Override
    public int getHeight() {
        return PREVIEW_SIZE + 10;
    }

    @Override
    public int getWidth(Font font) {
        return PREVIEW_SIZE;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        Snapshot snapshot = getSnapshot();
        if (snapshot == null) {
            return;
        }

        int previewX = x;
        int previewY = y + 10;
        int previewRight = previewX + PREVIEW_SIZE;
        int previewBottom = previewY + PREVIEW_SIZE;

        int background = 0xF0100010;
        int borderTop = 0x505000FF;
        int borderBottom = (borderTop & 0xFEFEFE) >> 1 | borderTop & 0xFF000000;

        guiGraphics.fill(previewX - 4, previewY - 4, previewRight + 4, previewBottom + 4, background);
        guiGraphics.fill(previewX - 3, previewY - 3, previewRight + 3, previewY - 2, borderTop);
        guiGraphics.fill(previewX - 3, previewBottom + 2, previewRight + 3, previewBottom + 3, borderBottom);
        guiGraphics.fill(previewX - 3, previewY - 2, previewX - 2, previewBottom + 2, borderTop);
        guiGraphics.fill(previewRight + 2, previewY - 2, previewRight + 3, previewBottom + 2, borderBottom);

        ClientSnapshots.INSTANCE.renderSnapshot(
            guiGraphics.pose(),
            snapshot,
            previewX,
            previewY,
            PREVIEW_SIZE,
            PREVIEW_SIZE
        );
    }

    private Snapshot getSnapshot() {
        Header header = BCBuildersItems.BLUEPRINT.get() != null && BCBuildersItems.TEMPLATE.get() != null
            ? ItemSnapshot.getHeader(blueprint)
            : null;
        if (header != null) {
            return ClientSnapshots.INSTANCE.getSnapshot(header.key);
        }

        if (BCBuildersItems.SCHEMATIC_SINGLE.get() == null) {
            return null;
        }
        ISchematicBlock schematicBlock = ItemSchematicSingle.getSchematicSafe(blueprint);
        if (schematicBlock == null) {
            return null;
        }

        Blueprint singleBlockBlueprint = new Blueprint();
        singleBlockBlueprint.size = new BlockPos(1, 1, 1);
        singleBlockBlueprint.offset = BlockPos.ZERO;
        singleBlockBlueprint.data = new int[] { 0 };
        singleBlockBlueprint.palette.add(schematicBlock);
        singleBlockBlueprint.computeKey();
        return singleBlockBlueprint;
    }
}
