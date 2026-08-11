package buildcraft.api.v2.guide;

import buildcraft.api.v2.registry.RegistrationContext;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/** Authoritative extension point for code-owned Guide Book content. */
public interface GuideService {
    void registerSection(GuideSection section, RegistrationContext context);
    void registerEntry(GuideEntry entry, RegistrationContext context);

    Optional<GuideSection> section(ResourceLocation id);
    Optional<GuideEntry> entry(ResourceLocation id);
    List<GuideSection> sections();
    List<GuideEntry> entries();
    List<GuideEntry> entries(ResourceLocation sectionId);
}
