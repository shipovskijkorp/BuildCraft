package buildcraft.compat.jei;

import java.util.ArrayList;
import java.util.List;

import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.world.item.ItemStack;

final class CraftingPhantomTransfer {
    private CraftingPhantomTransfer() {
    }

    static List<ItemStack> getCraftingGrid(IRecipeSlotsView recipeSlots) {
        List<IRecipeSlotView> inputs = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
        List<ItemStack> stacks = new ArrayList<>(9);
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = slot < inputs.size()
                    ? inputs.get(slot).getDisplayedItemStack().orElse(ItemStack.EMPTY)
                    : ItemStack.EMPTY;
            stacks.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }
        return stacks;
    }
}
