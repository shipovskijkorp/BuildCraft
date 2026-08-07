package buildcraft.silicon.recipe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 1.21.1 stores datapack recipe ids on RecipeHolder rather than on Recipe itself.
 *
 * The pre-1.21 regression test instantiated FacadeAssemblyRecipes directly to verify getId().
 * Doing that in a plain JUnit JVM initializes Minecraft's Recipe codecs before the game registries
 * are bootstrapped. More importantly, that is no longer the runtime path that carries the recipe id.
 *
 * Guard the actual 1.21.1 assembly-table path instead: it must take the id from RecipeHolder and use
 * that same id for the instruction sent/stored by BuildCraft and for RecipeManager lookup.
 */
public class FacadeAssemblyRecipesTester {
    private static final Pattern HOLDER_ID = Pattern.compile(
        "ResourceLocation\\s+recipeId\\s*=\\s*holder\\.id\\s*\\(\\s*\\)\\s*;"
    );
    private static final Pattern INSTRUCTION_ID = Pattern.compile(
        "new\\s+AssemblyInstruction\\s*\\(\\s*recipeId\\s*,\\s*recipe\\s*,"
    );
    private static final Pattern LOOKUP_ID = Pattern.compile(
        "getRecipeManager\\s*\\(\\s*\\)\\s*\\.\\s*byKey\\s*\\(\\s*recipeId\\s*\\)"
    );

    @Test
    void assemblyTableUsesRecipeHolderDatapackId() throws IOException {
        Path source = Path.of("src/main/java/buildcraft/silicon/tile/TileAssemblyTable.java");
        String tileAssemblyTable = Files.readString(source);

        Assertions.assertTrue(
            HOLDER_ID.matcher(tileAssemblyTable).find(),
            "Assembly Table must take the datapack recipe id from RecipeHolder.id() on 1.21.1"
        );
        Assertions.assertTrue(
            INSTRUCTION_ID.matcher(tileAssemblyTable).find(),
            "AssemblyInstruction must keep the RecipeHolder datapack id"
        );
        Assertions.assertTrue(
            LOOKUP_ID.matcher(tileAssemblyTable).find(),
            "Saved/synced assembly recipe ids must be resolved through RecipeManager.byKey(recipeId)"
        );
    }
}
