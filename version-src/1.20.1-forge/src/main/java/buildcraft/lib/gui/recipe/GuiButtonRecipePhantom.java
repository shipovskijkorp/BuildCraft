package buildcraft.lib.gui.recipe;

import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;

public class GuiButtonRecipePhantom extends RecipeButton {
    @Override
    public void init(RecipeCollection list, RecipeBookPage page) {
        super.init(new RecipeListPhantom(list), page);
    }
}
