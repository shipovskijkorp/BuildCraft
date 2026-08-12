package buildcraft.api.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import buildcraft.api.v2.content.BuildCraftContentIds;
import buildcraft.api.v2.energy.MjAmount;
import buildcraft.api.v2.fluid.FluidVariant;
import buildcraft.api.v2.guide.GuideEntry;
import buildcraft.api.v2.guide.GuidePages;
import buildcraft.api.v2.machine.BuiltInMachineProperties;
import buildcraft.api.v2.machine.MachineType;
import buildcraft.api.v2.pipe.ItemTransportProfile;
import buildcraft.api.v2.pipe.PipeType;
import buildcraft.api.v2.recipe.DistillationRecipeDefinition;
import buildcraft.api.v2.recipe.FluidIngredient;
import buildcraft.api.v2.worldgen.ResourceDepositRule;
import buildcraft.api.v2.worldgen.WorldTargetSelector;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ContentExtensionSurfaceTester {
    @Test
    void machineVariantCopiesBaseAndOverridesOnlyRequestedSettings() {
        MachineType base = MachineType.builder(id("quarry"))
            .component(id("miner"))
            .component(id("area"))
            .property(BuiltInMachineProperties.WORK_SPEED_MULTIPLIER, 1.0)
            .property(BuiltInMachineProperties.MAX_MJ_INPUT_PER_TICK, MjAmount.ofMj(256))
            .build();

        MachineType mk2 = MachineType.variant(id("quarry_mk2"), base)
            .property(BuiltInMachineProperties.WORK_SPEED_MULTIPLIER, 2.0)
            .component(id("overdrive"))
            .build();

        assertTrue(mk2.components().containsAll(Set.of(id("miner"), id("area"), id("overdrive"))));
        assertEquals(2.0, mk2.property(BuiltInMachineProperties.WORK_SPEED_MULTIPLIER).orElseThrow());
        assertEquals(MjAmount.ofMj(256), mk2.property(BuiltInMachineProperties.MAX_MJ_INPUT_PER_TICK).orElseThrow());
    }

    @Test
    void pipeVariantCopiesTransportProfileAndComponents() {
        PipeType base = PipeType.builder(id("base_pipe"))
            .itemProfile(new ItemTransportProfile(16, 8))
            .component(id("extractor"))
            .build();
        PipeType variant = PipeType.variant(id("fast_pipe"), base).component(id("accelerator")).build();
        assertTrue(variant.itemProfile().isPresent());
        assertTrue(variant.defaultComponents().containsAll(Set.of(id("extractor"), id("accelerator"))));
        assertEquals(base.id(), variant.archetypeId().orElseThrow());
        assertEquals(base.colorable(), variant.colorable());
    }

    @Test
    void guideEntriesAreDataLikeAndComposable() {
        GuideEntry entry = GuideEntry.builder(id("moon_oil"), id("moon"), "test.guide.moon_oil")
            .page(GuidePages.textKey("test.guide.moon_oil.text"))
            .page(GuidePages.link(id("quarry_mk2"), "test.guide.quarry_mk2"))
            .build();
        assertEquals(2, entry.pages().size());
        assertEquals("test.guide.moon_oil", entry.titleKey());
    }

    @Test
    void standardOilCanBeTargetedAtAddonDimension() {
        ResourceLocation moon = id("moon_dimension");
        ResourceDepositRule rule = ResourceDepositRule.builder(id("moon_oil"), BuildCraftContentIds.Worldgen.STANDARD_OIL)
            .target(WorldTargetSelector.builder().dimension(moon).build())
            .frequencyMultiplier(0.5)
            .build();
        assertEquals(BuildCraftContentIds.Worldgen.STANDARD_OIL, rule.profile());
        assertTrue(rule.target().dimensions().contains(moon));
        assertFalse(rule.target().matches(id("other_dimension"), id("plains"), ignored -> false, ignored -> false));
    }

    @Test
    void distillationBuilderAvoidsRawMicroMjAndLargeConstructor() {
        FluidVariant crude = FluidVariant.of(id("crude"));
        FluidVariant gas = FluidVariant.of(id("gas"));
        DistillationRecipeDefinition recipe = DistillationRecipeDefinition.builder()
            .input(FluidIngredient.exact(crude, 100))
            .gas(gas, 40)
            .powerMj(25)
            .build();
        assertEquals(MjAmount.ofMj(25), recipe.powerRequired());
        assertEquals(40L, recipe.gasOutput().amount().milliBuckets());
    }

    private static ResourceLocation id(String path) {
        return java.util.Objects.requireNonNull(ResourceLocation.tryParse("api_v2_test:" + path));
    }
}
