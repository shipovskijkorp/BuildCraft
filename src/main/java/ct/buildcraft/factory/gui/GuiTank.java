/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package ct.buildcraft.factory.gui;

import com.mojang.blaze3d.vertex.PoseStack;

import ct.buildcraft.factory.container.ContainerTank;
import ct.buildcraft.lib.gui.ContainerScreenBase;
import ct.buildcraft.lib.gui.component.TankComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.fluids.FluidType;

public class GuiTank extends ContainerScreenBase<ContainerTank> {
    private static final ResourceLocation TEXTURE_BASE = new ResourceLocation("buildcraftfactory:textures/gui/tank.png");
    private static final int SIZE_X = 176;
    private static final int SIZE_Y = 181;

    public GuiTank(ContainerTank menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 1, TEXTURE_BASE);
        imageWidth = SIZE_X;
        imageHeight = SIZE_Y;
        inventoryLabelY = SIZE_Y - 94;
        add(new TankComponent(
            80, 18, 16, 64,
            16 * FluidType.BUCKET_VOLUME,
            176, 0
        ), true);
        setup(menu.data);
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        super.render(pose, mouseX, mouseY, partialTick);
        renderTooltip(pose, mouseX, mouseY);
    }
}
