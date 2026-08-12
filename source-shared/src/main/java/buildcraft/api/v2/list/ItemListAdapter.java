package buildcraft.api.v2.list;

import net.minecraft.world.item.ItemStack;

/**
 * Adapter for item stacks that themselves encode a list/filter definition.
 * Implementations should treat {@code listStack} as read-only while matching.
 */
public interface ItemListAdapter {
    boolean supports(ItemStack listStack);
    boolean matches(ItemStack listStack, ItemStack target);
}
