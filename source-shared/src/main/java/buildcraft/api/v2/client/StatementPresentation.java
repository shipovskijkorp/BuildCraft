package buildcraft.api.v2.client;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record StatementPresentation(ResourceLocation statementId, String titleKey, String descriptionKey, ResourceLocation icon) {
    public StatementPresentation {
        Objects.requireNonNull(statementId, "statementId"); Objects.requireNonNull(titleKey, "titleKey");
        descriptionKey = descriptionKey == null ? "" : descriptionKey;
    }
}
