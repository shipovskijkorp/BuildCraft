package buildcraft.api.v2.recipe;

import buildcraft.api.v2.reload.DefinitionProvenance;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record RecipeMatch<T extends RecipeDefinition>(
    ResourceLocation id, T recipe, DefinitionProvenance provenance
) {
    public RecipeMatch {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(recipe, "recipe");
        Objects.requireNonNull(provenance, "provenance");
    }
}
