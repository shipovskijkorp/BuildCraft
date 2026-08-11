package dev.bcce.apifixture;

import buildcraft.api.v2.content.BuildCraftContent;
import buildcraft.api.v2.content.BuildCraftContentIds;
import buildcraft.api.v2.content.ContentRegistrar;
import buildcraft.api.v2.energy.MjAmount;
import buildcraft.api.v2.fluid.FluidVariant;
import buildcraft.api.v2.guide.GuideEntry;
import buildcraft.api.v2.guide.GuidePages;
import buildcraft.api.v2.guide.GuideSection;
import buildcraft.api.v2.machine.BuiltInMachineProperties;
import buildcraft.api.v2.recipe.FluidIngredient;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * Compile-only acceptance example for the high-level content extension API.
 *
 * <p>This is intentionally written like a small real addon: no BuildCraft implementation,
 * loader API, block entity subclass, or direct registry plumbing is required.
 */
public final class EasyContentFixtureAddon {
    private EasyContentFixtureAddon() {
    }

    public static void register() {
        ContentRegistrar bc = BuildCraftContent.addon("moonbuildcraft");

        ResourceLocation guideSection = bc.id("moon_industry");
        bc.guideSection(GuideSection.builder(guideSection, "moonbuildcraft.guide.moon_industry")
            .icon(id("moonbuildcraft:moon_quarry"))
            .order(200)
            .build());
        bc.guideEntry(GuideEntry.builder(bc.id("moon_oil"), guideSection, "moonbuildcraft.guide.moon_oil")
            .icon(id("buildcraftenergy:oil"))
            .page(GuidePages.textKey("moonbuildcraft.guide.moon_oil.text"))
            .page(GuidePages.link(bc.id("quarry_mk2"), "moonbuildcraft.guide.quarry_mk2"))
            .build());

        // Reuse BuildCraft's standard oil generator in the addon's dimension.
        bc.oilInDimension("moon_oil_generation", id("moonbuildcraft:moon"), 0.75);

        // Add a Distiller recipe without touching RefineryRecipeRegistry or Forge FluidStack.
        FluidVariant crude = FluidVariant.of(id("moonbuildcraft:moon_crude"));
        FluidVariant gas = FluidVariant.of(id("moonbuildcraft:helium_mix"));
        bc.distillation("moon_crude_distillation", recipe -> recipe
            .input(FluidIngredient.exact(crude, 100))
            .gas(gas, 35)
            .powerMj(20));

        // Build Quarry Mk2 from the standard Quarry definition, overriding only what differs.
        bc.machineVariant("quarry_mk2", BuildCraftContentIds.Machines.QUARRY, machine -> machine
            .property(BuiltInMachineProperties.WORK_SPEED_MULTIPLIER, 2.0)
            .property(BuiltInMachineProperties.MAX_MJ_INPUT_PER_TICK, MjAmount.ofMj(512))
            .component(bc.id("overdrive_component")));
    }

    private static ResourceLocation id(String value) {
        return Objects.requireNonNull(ResourceLocation.tryParse(value));
    }
}
