package buildcraft.lib.recipe;

import java.util.Objects;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

/**
 * Mutable wrapper around an immutable vanilla {@link Ingredient}.
 *
 * <p>Ingredient is no longer intended to be subclassed on modern Minecraft/NeoForge.
 * Call {@link #asIngredient()} when an actual Ingredient instance is required.</p>
 */
public final class MutableIngredient implements Predicate<ItemStack> {
    private Ingredient delegate = Ingredient.EMPTY;

    public void setDelegate(@Nonnull ItemLike item) {
        delegate = Ingredient.of(Objects.requireNonNull(item, "item"));
    }

    public void setDelegate(@Nonnull TagKey<Item> tag) {
        delegate = Ingredient.of(Objects.requireNonNull(tag, "tag"));
    }

    public void setDelegate(@Nonnull Ingredient ingredient) {
        delegate = Objects.requireNonNull(ingredient, "ingredient");
    }

    public Ingredient asIngredient() {
        return delegate;
    }

    public ItemStack[] getItems() {
        return delegate.getItems();
    }

    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean test(ItemStack stack) {
        return delegate.test(stack);
    }

    @Override
    public Predicate<ItemStack> and(Predicate<? super ItemStack> other) {
        return delegate.and(other);
    }

    @Override
    public Predicate<ItemStack> negate() {
        return delegate.negate();
    }

    @Override
    public Predicate<ItemStack> or(Predicate<? super ItemStack> other) {
        return delegate.or(other);
    }
}
