package buildcraft.api.v2.item;

import net.minecraft.world.item.ItemStack;

/** Optional slot-aware view for machines whose slot filters are meaningful to integrations. */
public interface SlottedItemPort extends ItemPort {
    int slots();
    ItemStack stackInSlot(int slot);

    /** Returns the effective filter for a slot, not necessarily a single representative stack. */
    default ItemMatcher filter(int slot) {
        return ItemMatcher.any();
    }
}
