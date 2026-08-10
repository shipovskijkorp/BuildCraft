package buildcraft.api.v2.reload;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record DefinitionEntry<V>(ResourceLocation id, V value, DefinitionProvenance provenance) {
    public DefinitionEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(provenance, "provenance");
    }
}
