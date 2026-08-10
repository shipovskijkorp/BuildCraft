package buildcraft.api.v2.recipe;

import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

/** Programmatic Integration Table recipe contract. */
public interface IntegrationRecipeDefinition extends RecipeDefinition {
    @Override
    default Kind kind() { return Kind.INTEGRATION; }

    ItemStack output(ItemStack target, NonNullList<ItemStack> toIntegrate);
    List<CountedIngredient> requirements(ItemStack output);
    long requiredMicroJoules(ItemStack output);
    CountedIngredient centerIngredient();
}
