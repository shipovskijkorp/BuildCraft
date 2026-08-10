package buildcraft.lib.api.v2;

import buildcraft.api.v2.fluid.FluidVariant;
import buildcraft.api.v2.fuels.CoolantProfile;
import buildcraft.api.v2.fuels.EnergyFluidRegistration;
import buildcraft.api.v2.fuels.FuelProfile;
import buildcraft.api.v2.fuels.FluidSelector;
import buildcraft.api.v2.reload.DefinitionProvenance;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EnergyFluidRegistryImplTester {
    @Test
    public void codeBaselineAndDataOverridePublishAtomically() {
        EnergyFluidRegistryImpl service = new EnergyFluidRegistryImpl();
        service.register(
            id("fuel"),
            FuelProfile.clean(FluidSelector.fluid(id("oil")), 10, 1000),
            new DefinitionProvenance("buildcraft", "code", 0)
        );

        assertEquals(10, service.findFuel(FluidVariant.of(id("oil")), (fluid, tag) -> false)
            .orElseThrow().profile().powerPerTickMicroMj());

        assertTrue(service.reloadData(List.of(new EnergyFluidRegistration(
            id("fuel"),
            FuelProfile.clean(FluidSelector.fluid(id("oil")), 20, 1000),
            new DefinitionProvenance("pack", "data/buildcraft/fuel.json", 100)
        ))).published());

        assertEquals(20, service.findFuel(FluidVariant.of(id("oil")), (fluid, tag) -> false)
            .orElseThrow().profile().powerPerTickMicroMj());
    }

    @Test
    public void invalidDataReloadKeepsLastKnownGoodDefinitions() {
        EnergyFluidRegistryImpl service = new EnergyFluidRegistryImpl();
        service.register(
            id("coolant"),
            CoolantProfile.constant(FluidSelector.fluid(id("water")), 0.0023),
            new DefinitionProvenance("buildcraft", "code", 0)
        );

        List<EnergyFluidRegistration> ambiguous = List.of(
            new EnergyFluidRegistration(id("x"), FuelProfile.clean(FluidSelector.fluid(id("a")), 1, 1),
                new DefinitionProvenance("a", "a.json", 10)),
            new EnergyFluidRegistration(id("x"), FuelProfile.clean(FluidSelector.fluid(id("b")), 2, 2),
                new DefinitionProvenance("b", "b.json", 10))
        );
        assertFalse(service.reloadData(ambiguous).published());
        assertTrue(service.findCoolant(FluidVariant.of(id("water")), (fluid, tag) -> false).isPresent());
        assertEquals(1, service.snapshot().definitions().size());
    }

    @Test
    public void profileLookupUsesPriorityThenStableId() {
        EnergyFluidRegistryImpl service = new EnergyFluidRegistryImpl();
        service.register(id("low"), FuelProfile.clean(FluidSelector.fluid(id("oil")), 1, 1),
            new DefinitionProvenance("a", "code", 0));
        service.register(id("high"), FuelProfile.clean(FluidSelector.fluid(id("oil")), 2, 2),
            new DefinitionProvenance("b", "code", 5));
        assertEquals(id("high"), service.findFuel(FluidVariant.of(id("oil")), (fluid, tag) -> false).orElseThrow().id());
    }

    private static ResourceLocation id(String path) {
        return Objects.requireNonNull(ResourceLocation.tryParse("test:" + path));
    }
}
