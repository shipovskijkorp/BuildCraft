package buildcraft.api.v2.fuels;

import buildcraft.api.v2.reload.DefinitionProvenance;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** One code- or data-owned fuel/coolant definition. */
public record EnergyFluidRegistration(
    ResourceLocation id,
    EnergyFluidDefinition definition,
    DefinitionProvenance provenance
) {
    public EnergyFluidRegistration {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(provenance, "provenance");
    }
}
