package buildcraft.api.v2.recipe;

import java.util.Objects;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

/** Typed replacement for the legacy IngredientStack raw-Object API. */
public final class CountedIngredient {
    private final Ingredient ingredient;
    private final int count;

    private CountedIngredient(Ingredient ingredient, int count) {
        this.ingredient = Objects.requireNonNull(ingredient, "ingredient");
        if (count <= 0) throw new IllegalArgumentException("count must be > 0");
        this.count = count;
    }

    public static CountedIngredient of(Ingredient ingredient, int count) {
        return new CountedIngredient(ingredient, count);
    }

    public static CountedIngredient of(ItemLike item, int count) {
        return of(Ingredient.of(Objects.requireNonNull(item, "item")), count);
    }

    public static CountedIngredient of(ItemStack stack, int count) {
        Objects.requireNonNull(stack, "stack");
        if (stack.isEmpty()) throw new IllegalArgumentException("stack must not be empty");
        return of(Ingredient.of(stack.copy()), count);
    }

    public static CountedIngredient of(TagKey<Item> tag, int count) {
        return of(Ingredient.of(Objects.requireNonNull(tag, "tag")), count);
    }

    public Ingredient ingredient() { return ingredient; }
    public int count() { return count; }

    public boolean test(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getCount() >= count && ingredient.test(stack);
    }
}
