package buildcraft.api.v2.recipe;

import buildcraft.api.v2.reload.DefinitionProvenance;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record MachineRecipeRegistration(
    ResourceLocation id, RecipeDefinition definition, DefinitionProvenance provenance
) {
    public MachineRecipeRegistration {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(provenance, "provenance");
    }
}
