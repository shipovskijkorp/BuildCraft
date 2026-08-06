package buildcraft.core;

import net.minecraft.data.PackOutput;

/** @deprecated Kept as a source-compatible alias for older datagen entry points. */
@Deprecated(forRemoval = true)
public class BCCoreRecipesProvider extends BCCoreRecipes {
    public BCCoreRecipesProvider(PackOutput output) {
        super(output);
    }
}
