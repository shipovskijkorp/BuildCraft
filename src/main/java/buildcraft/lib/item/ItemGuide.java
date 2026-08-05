/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 */
package buildcraft.lib.item;

import javax.annotation.Nullable;

import buildcraft.lib.client.guide.GuiGuide;
import buildcraft.lib.misc.AdvancementUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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

    /** Stored on the individual guide stack so two books can remember different places and options. */
    private static final String TAG_GUIDE_STATE = "BuildCraftGuideState";
    private static final String TAG_VERSION = "Version";
    private static final String TAG_SHOW_LORE = "ShowLore";
    private static final String TAG_SHOW_HINTS = "ShowHints";
    private static final String TAG_SORT_MODE = "SortMode";
    private static final String TAG_DOCUMENT = "Document";
    private static final String TAG_ENTRY = "Entry";
    private static final String TAG_SPREAD = "Spread";
    private static final int GUIDE_STATE_VERSION = 1;

    public ItemGuide(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            AdvancementUtil.unlockAdvancement(player, ADVANCEMENT);
        } else {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openGuide(stack, hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        // Preserve the original BuildCraft 8 translation key.
        return "item.buildcraft.guide.name";
    }

    /** Reads the UI state stored on this exact guide stack. */
    public static GuideState readGuideState(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(TAG_GUIDE_STATE, Tag.TAG_COMPOUND)) {
            return GuideState.DEFAULT;
        }
        CompoundTag tag = root.getCompound(TAG_GUIDE_STATE);
        if (tag.getInt(TAG_VERSION) != GUIDE_STATE_VERSION) {
            return GuideState.DEFAULT;
        }

        boolean showLore = !tag.contains(TAG_SHOW_LORE) || tag.getBoolean(TAG_SHOW_LORE);
        boolean showHints = tag.getBoolean(TAG_SHOW_HINTS);
        String sortMode = tag.contains(TAG_SORT_MODE) ? tag.getString(TAG_SORT_MODE) : "TYPE";
        boolean document = tag.getBoolean(TAG_DOCUMENT);
        ResourceLocation entry = null;
        if (document && tag.contains(TAG_ENTRY)) {
            try {
                entry = new ResourceLocation(tag.getString(TAG_ENTRY));
            } catch (RuntimeException ignored) {
                document = false;
            }
        }
        int spread = Math.max(0, tag.getInt(TAG_SPREAD));
        return new GuideState(showLore, showHints, sortMode, document, entry, spread);
    }

    /** Writes UI state to this exact guide stack. */
    public static void writeGuideState(ItemStack stack, GuideState state) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemGuide)) {
            return;
        }
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_VERSION, GUIDE_STATE_VERSION);
        tag.putBoolean(TAG_SHOW_LORE, state.showLore);
        tag.putBoolean(TAG_SHOW_HINTS, state.showHints);
        tag.putString(TAG_SORT_MODE, state.sortMode);
        tag.putBoolean(TAG_DOCUMENT, state.document && state.entry != null);
        if (state.document && state.entry != null) {
            tag.putString(TAG_ENTRY, state.entry.toString());
        }
        tag.putInt(TAG_SPREAD, Math.max(0, state.spread));
        stack.getOrCreateTag().put(TAG_GUIDE_STATE, tag);
    }

    /** Immutable payload shared by the screen, item NBT and the client-to-server update packet. */
    public static final class GuideState {
        public static final GuideState DEFAULT = new GuideState(true, false, "TYPE", false, null, 0);

        public final boolean showLore;
        public final boolean showHints;
        public final String sortMode;
        public final boolean document;
        public final @Nullable ResourceLocation entry;
        public final int spread;

        public GuideState(boolean showLore, boolean showHints, String sortMode, boolean document,
            @Nullable ResourceLocation entry, int spread) {
            this.showLore = showLore;
            this.showHints = showHints;
            this.sortMode = sortMode == null || sortMode.isEmpty() ? "TYPE" : sortMode;
            this.document = document && entry != null;
            this.entry = entry;
            this.spread = Math.max(0, spread);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHooks {
        private static void openGuide(ItemStack stack, InteractionHand hand) {
            GuiGuide.open(stack, hand);
        }
    }
}
