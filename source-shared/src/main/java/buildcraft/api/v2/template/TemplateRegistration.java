package buildcraft.api.v2.template;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record TemplateRegistration(ResourceLocation id, int priority, TemplateHandler handler) {
    public TemplateRegistration {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(handler, "handler");
    }
}
