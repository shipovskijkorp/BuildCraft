package buildcraft.lib.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;

import buildcraft.core.BCCoreItems;
import buildcraft.transport.BCTransportSprites;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class EmzuliButton extends AbstractComponent{
	
	private static final ResourceLocation TEXTURE_BASE = BCTransportSprites.EMZULI_GUI;
	
	public EmzuliButton(int x, int y) {
		super(x, y, 18, 18);
	}
	
	
	
	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, AbstractContainerScreen<?> screen) {
		int leftpos = screen.getGuiLeft() + x;
		int toppos = screen.getGuiTop() + y-2;
		RenderSystem.setShaderTexture(0, TEXTURE_BASE);
		int num = data.get(offset);
		
		guiGraphics.blit(TEXTURE_BASE, leftpos, toppos, 176, (isPressing ? 20 : 0), 20, 20);
		if(num == 128) {
			guiGraphics.blit(TEXTURE_BASE, leftpos + 2, toppos + 2, 176, 40, 16, 16);
			return;
		}
		Item brush = BCCoreItems.PAINT_BRUSHS.get(DyeColor.byId(num));
		guiGraphics.renderItem(new ItemStack(brush), leftpos + 2, toppos + 2);

	}

	@Override
	public int getNeedDataSize() {
		return 1;
	}

	
}
