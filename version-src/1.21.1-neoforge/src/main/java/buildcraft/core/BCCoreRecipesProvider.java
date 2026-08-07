package buildcraft.core;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;

/** @deprecated Kept as a source-compatible alias for older datagen entry points. */
@Deprecated(forRemoval = true)
public class BCCoreRecipesProvider extends BCCoreRecipes {
    public BCCoreRecipesProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }
}
