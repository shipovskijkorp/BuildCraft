package buildcraft.api.v2.list;

import net.minecraft.world.item.ItemStack;

/** Optional addon hook for special list semantics not expressible as ItemMatcher. */
@FunctionalInterface
public interface ListMatchAdapter {
    boolean matches(ItemList list, ItemStack stack);
}
