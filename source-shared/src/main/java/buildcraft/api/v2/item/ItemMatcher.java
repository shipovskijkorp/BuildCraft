package buildcraft.api.v2.item;

import net.minecraft.world.item.ItemStack;

/** Loader-neutral item matching rule. */
@FunctionalInterface
public interface ItemMatcher {
    boolean matches(ItemStack stack);

    static ItemMatcher any() {
        return stack -> stack != null && !stack.isEmpty();
    }

    static ItemMatcher none() {
        return stack -> false;
    }
}
