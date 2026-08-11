package buildcraft.api.v2.item;

import java.util.List;
import java.util.Objects;
import net.minecraft.world.item.ItemStack;

/** Loader-neutral item matching rule. */
@FunctionalInterface
public interface ItemMatcher {
    boolean matches(ItemStack stack);

    /** Optional representative stacks for GUIs and diagnostics. */
    default List<ItemStack> examples() {
        return List.of();
    }

    static ItemMatcher any() {
        return stack -> stack != null && !stack.isEmpty();
    }

    static ItemMatcher none() {
        return stack -> false;
    }

    default ItemMatcher and(ItemMatcher other) {
        Objects.requireNonNull(other, "other");
        ItemMatcher before = this;
        return stack -> before.matches(stack) && other.matches(stack);
    }

    default ItemMatcher or(ItemMatcher other) {
        Objects.requireNonNull(other, "other");
        ItemMatcher before = this;
        return stack -> before.matches(stack) || other.matches(stack);
    }

    default ItemMatcher negate() {
        ItemMatcher before = this;
        return stack -> !before.matches(stack);
    }
}
