/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 */
package buildcraft.lib.item;

import buildcraft.lib.client.guide.GuiGuide;
import buildcraft.lib.misc.AdvancementUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;

/** The native BuildCraft guide book, using the original BC8 book artwork. */
public class ItemGuide extends Item {
    private static final ResourceLocation ADVANCEMENT = new ResourceLocation("buildcraftcore", "guide");

    public ItemGuide(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            AdvancementUtil.unlockAdvancement(player, ADVANCEMENT);
        } else {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientHooks::openGuide);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        // Preserve the original BuildCraft 8 translation key.
        return "item.buildcraft.guide.name";
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHooks {
        private static void openGuide() {
            GuiGuide.open();
        }
    }
}
