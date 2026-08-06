/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.item;

import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nonnull;

import buildcraft.api.facades.FacadeAPI;
import buildcraft.api.facades.FacadeType;
import buildcraft.api.facades.IFacade;
import buildcraft.api.facades.IFacadeItem;
import buildcraft.api.transport.IItemPluggable;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.lib.item.ICreativeTabItemProvider;
import buildcraft.lib.misc.BlockUtil;
import buildcraft.lib.misc.LocaleUtil;
import buildcraft.lib.misc.ItemStackUtil;
import buildcraft.lib.misc.NBTUtilBC;
import buildcraft.lib.misc.SoundUtil;
import buildcraft.lib.misc.StackUtil;
import buildcraft.silicon.BCSiliconPlugs;
import buildcraft.silicon.plug.FacadeBlockStateInfo;
import buildcraft.silicon.plug.FacadeInstance;
import buildcraft.silicon.plug.FacadePhasedState;
import buildcraft.silicon.plug.FacadeStateManager;
import buildcraft.silicon.plug.PluggableFacade;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

public class ItemPluggableFacade extends Item implements IItemPluggable, IFacadeItem, ICreativeTabItemProvider {
    public ItemPluggableFacade() {
        super(new Item.Properties().stacksTo(64));
        FacadeAPI.facadeItem = this;
//        setHasSubtypes(true);
    }

    @Nonnull
    public ItemStack createItemStack(FacadeInstance state) {
        ItemStack item = new ItemStack(this);
        CompoundTag nbt = NBTUtilBC.getItemData(item);
        nbt.put("facade", state.writeToNbt());
        ItemStackUtil.setCustomData(item, nbt);
        return item;
    }

    public static FacadeInstance getStates(@Nonnull ItemStack item) {
        CompoundTag nbt = NBTUtilBC.getItemData(item);

        String strPreview = nbt.getString("preview");
        if ("basic".equalsIgnoreCase(strPreview)) {
            return FacadeInstance.createSingle(FacadeStateManager.previewState, false);
        }

        if (!nbt.contains("facade") && nbt.contains("states")) {
            ListTag states = nbt.getList("states", Tag.TAG_COMPOUND);
            if (states.size() > 0) {
                // Only migrate if we actually have a facade to migrate.
                boolean isHollow = states.getCompound(0).getBoolean("isHollow");
                CompoundTag tagFacade = new CompoundTag();
                tagFacade.putBoolean("isHollow", isHollow);
                tagFacade.put("states", states);
                nbt.put("facade", tagFacade);
                ItemStackUtil.setCustomData(item, nbt);
            }
        }

        return FacadeInstance.readFromNbt(nbt.getCompound("facade"));
    }

    @Nonnull
    @Override
    public ItemStack getFacadeForBlock(BlockState state) {
        FacadeBlockStateInfo info = FacadeStateManager.validFacadeStates.get(state);
        if (info == null) {
            return StackUtil.EMPTY;
        } else {
            return createItemStack(FacadeInstance.createSingle(info, false));
        }
    }

    @Override
    public PipePluggable onPlace(@Nonnull ItemStack stack, IPipeHolder holder, Direction side, Player player,
        InteractionHand hand) {
        FacadeInstance fullState = getStates(stack);
        SoundUtil.playBlockPlace(holder.getPipeWorld(), holder.getPipePos(), fullState.phasedStates[0].stateInfo.state);
        return new PluggableFacade(BCSiliconPlugs.facade, holder, side, fullState);
    }
    
    

    @Override
    public void addCreativeTabItems(Consumer<ItemStack> output) {
		// Add a single phased facade as a default
        // check if the data is present as we only process in post-init
        FacadeBlockStateInfo stone = FacadeStateManager.getInfoForBlock(Blocks.STONE);
        FacadeBlockStateInfo planks = FacadeStateManager.getInfoForBlock(Blocks.OAK_PLANKS);
        FacadeBlockStateInfo log = FacadeStateManager.getInfoForBlock(Blocks.OAK_LOG);
        if (stone != null && planks != null && log != null) {
            FacadePhasedState[] states = {
                stone.createPhased(null),
                planks.createPhased(DyeColor.RED),
                log.createPhased(DyeColor.CYAN),
            };
            output.accept(createItemStack(new FacadeInstance(states, false)));
        }

        for (FacadeBlockStateInfo info : FacadeStateManager.validFacadeStates.values()) {
            if (!ForgeRegistries.BLOCKS.containsValue(info.state.getBlock())) {
                // Forge can de-register blocks if the server a client is connected to
                // doesn't have the mods that created them.
                continue;
            }
            if (info.isVisible) {
                output.accept(createItemStack(FacadeInstance.createSingle(info, false)));
                output.accept(createItemStack(FacadeInstance.createSingle(info, true)));
            }
        }
	}

	@Override
	public Component getName(ItemStack stack) {
        FacadeInstance fullState = getStates(stack);
        if (fullState.type == FacadeType.Basic) {
            return Component.translatable(
                "item.buildcraftsilicon.facade.named",
                Component.translatable("item.buildcraftsilicon.facade"),
                getFacadeStateDisplayName(fullState.phasedStates[0])
            );
        } else {
            return Component.translatable("item.buildcraftsilicon.facade_phased");
        }
	}

    public static Component getFacadeStateDisplayName(FacadePhasedState state) {
        ItemStack assumedStack = state.stateInfo.requiredStack;
        return assumedStack.getHoverName();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
    	 FacadeInstance states = getStates(stack);
         if (states.type == FacadeType.Phased) {
             FacadePhasedState defaultState = null;
             for (FacadePhasedState state : states.phasedStates) {
                 if (state.activeColour == null) {
                     defaultState = state;
                     continue;
                 }
                 tooltip.add(Component.translatable(
                     "item.buildcraftsilicon.facade_phased.state",
                     LocaleUtil.localizeColourComponent(state.activeColour),
                     getFacadeStateDisplayName(state)
                 ));
             }
             if (defaultState != null) {
                 tooltip.add(1, Component.translatable(
                     "item.buildcraftsilicon.facade_phased.state_default",
                     getFacadeStateDisplayName(defaultState)
                 ));
             }
         } else {
             if (flag.isAdvanced()) {
                 tooltip.add(Component.literal(ForgeRegistries.BLOCKS.getKey(states.phasedStates[0].stateInfo.state.getBlock()).toString()));
             }
             String propertiesStart = ChatFormatting.GRAY + "" + ChatFormatting.ITALIC;
             FacadeBlockStateInfo info = states.phasedStates[0].stateInfo;
             BlockUtil.getPropertiesStringMap(info.state, info.varyingProperties)
                 .forEach((name, value) -> tooltip.add(Component.literal(propertiesStart + name + " = " + value)));
         }
	}

    // IFacadeItem

    @Override
    public ItemStack createFacadeStack(IFacade facade) {
        return createItemStack((FacadeInstance) facade);
    }

    @Override
    public IFacade getFacade(ItemStack facade) {
        return getStates(facade);
    }
}
