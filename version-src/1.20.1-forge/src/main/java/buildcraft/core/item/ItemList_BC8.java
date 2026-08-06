/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.core.item;

import java.util.List;

import javax.annotation.Nonnull;

import buildcraft.api.items.IList;
import buildcraft.core.list.ContainerList;
import buildcraft.lib.list.ListHandler;
import buildcraft.lib.misc.AdvancementUtil;
import buildcraft.lib.misc.NBTUtilBC;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkHooks;

public class ItemList_BC8 extends Item implements IList, MenuProvider {
    private static final ResourceLocation ADVANCEMENT = new ResourceLocation("buildcraftcore:list");
    public ItemList_BC8(Item.Properties prop) {//stack to 1
        super(prop);
    }
    
    

    @Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        AdvancementUtil.unlockAdvancement(player, ADVANCEMENT);
        if (player instanceof ServerPlayer serverPlayer) {
            MenuProvider provider = new SimpleMenuProvider(
                (id, inventory, menuPlayer) -> new ContainerList(id, inventory, hand),
                getDisplayName()
            );
            NetworkHooks.openScreen(serverPlayer, provider, buffer -> buffer.writeEnum(hand));
        }
        return new InteractionResultHolder<ItemStack>(InteractionResult.SUCCESS, player.getItemInHand(hand));
	}

/*    @Override
    @OnlyIn(Dist.CLIENT)
    public void addModelVariants(TIntObjectHashMap<ModelResourceLocation> variants) {
        addVariant(variants, 0, "clean");
        addVariant(variants, 1, "used");
    }*/

/*    @Override
    public int getMetadata(ItemStack stack) {
        return ListHandler.hasItems(StackUtil.asNonNull(stack)) ? 1 : 0;
    }*/
    
    // IList

    @Override
    @OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag flag) {
		String name = getLabelName(stack);
        if (StringUtil.isNullOrEmpty(name)) return;
        tooltip.add(Component.literal(name).withStyle(ChatFormatting.ITALIC));
	}



    //TODO ItemStack:getHoverName
	@Override
    public String getLabelName(@Nonnull ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getString("label") : "";
    }

    @Override
    public boolean setLabelName(@Nonnull ItemStack stack, String name) {
        NBTUtilBC.getItemData(stack).putString("label", name);
        return true;
    }

    @Override
    public boolean matches(@Nonnull ItemStack stackList, @Nonnull ItemStack item) {
        return ListHandler.matches(stackList, item);
    }



	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		InteractionHand hand = player.getMainHandItem().getItem() == this
            ? InteractionHand.MAIN_HAND
            : InteractionHand.OFF_HAND;
        return new ContainerList(id, inv, hand);
	}



	@Override
	public Component getDisplayName() {
		return this.getDescription();
	}
}
