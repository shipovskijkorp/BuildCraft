package buildcraft.api.v2.statement;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record ParameterSpec(ResourceLocation slotId, ResourceLocation typeId, boolean required) {
    public ParameterSpec {
        Objects.requireNonNull(slotId, "slotId");
        Objects.requireNonNull(typeId, "typeId");
    }
}
