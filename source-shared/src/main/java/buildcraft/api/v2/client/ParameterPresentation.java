package buildcraft.api.v2.client;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/** Data-only description of a statement parameter editor. */
public record ParameterPresentation(
    ResourceLocation parameterTypeId,
    String titleKey,
    String descriptionKey,
    ResourceLocation icon,
    ResourceLocation editorTypeId
) {
    public ParameterPresentation {
        Objects.requireNonNull(parameterTypeId, "parameterTypeId");
        titleKey = Objects.requireNonNull(titleKey, "titleKey");
        descriptionKey = descriptionKey == null ? "" : descriptionKey;
        Objects.requireNonNull(editorTypeId, "editorTypeId");
    }

    public Optional<ResourceLocation> iconId() { return Optional.ofNullable(icon); }
}
