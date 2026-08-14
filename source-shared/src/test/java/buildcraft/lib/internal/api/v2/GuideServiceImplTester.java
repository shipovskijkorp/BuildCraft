package buildcraft.lib.internal.api.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import buildcraft.api.v2.guide.GuideEntry;
import buildcraft.api.v2.guide.GuidePages;
import buildcraft.api.v2.guide.GuideSection;
import buildcraft.api.v2.registry.RegistrationContext;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class GuideServiceImplTester {
    @Test
    void keepsAddonOwnershipAndSectionOrderingForTheNativeGuideUi() {
        GuideServiceImpl service = new GuideServiceImpl();
        RegistrationContext moon = () -> "moonbuildcraft";
        RegistrationContext orbital = () -> "orbitaltech";

        GuideSection later = GuideSection.builder(id("moonbuildcraft", "machines"), "guide.moon.machines")
            .order(20)
            .build();
        GuideSection first = GuideSection.builder(id("orbitaltech", "power"), "guide.orbital.power")
            .order(10)
            .build();
        service.registerSection(later, moon);
        service.registerSection(first, orbital);

        GuideEntry moonEntry = GuideEntry.builder(
                id("moonbuildcraft", "quarry"), later.id(), "guide.moon.quarry")
            .order(5)
            .page(GuidePages.text("Moon quarry"))
            .build();
        service.registerEntry(moonEntry, moon);

        assertEquals(List.of(first, later), service.sections());
        assertEquals("moonbuildcraft", service.ownerOfEntry(moonEntry.id()).orElseThrow());
        assertEquals("orbitaltech", service.ownerOfSection(first.id()).orElseThrow());
    }

    private static ResourceLocation id(String namespace, String path) {
        return java.util.Objects.requireNonNull(ResourceLocation.tryParse(namespace + ":" + path));
    }
}
