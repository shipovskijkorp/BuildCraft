package buildcraft.api.v2.client;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/** Client-facing metadata kept free of net.minecraft.client classes. */
public record ContentPresentation(ResourceLocation contentId, String titleKey, String descriptionKey, ResourceLocation icon, ResourceLocation model) {
    public ContentPresentation {
        Objects.requireNonNull(contentId, "contentId");
        titleKey = Objects.requireNonNull(titleKey, "titleKey");
        descriptionKey = descriptionKey == null ? "" : descriptionKey;
    }
    public Optional<ResourceLocation> iconId() { return Optional.ofNullable(icon); }
    public Optional<ResourceLocation> modelId() { return Optional.ofNullable(model); }
}
