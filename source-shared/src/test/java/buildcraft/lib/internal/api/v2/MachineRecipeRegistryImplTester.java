package buildcraft.lib.internal.api.v2;

import buildcraft.api.v2.fluid.FluidVariant;
import buildcraft.api.v2.fluid.FluidVolume;
import buildcraft.api.v2.recipe.DistillationRecipeDefinition;
import buildcraft.api.v2.recipe.FluidIngredient;
import buildcraft.lib.internal.api.v2.recipe.MachineRecipeRegistration;
import buildcraft.api.v2.reload.DefinitionProvenance;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MachineRecipeRegistryImplTester {
    @Test
    public void recipeLookupUsesPriorityAndDataReloadIsAtomic() {
        MachineRecipeRegistryImpl service = new MachineRecipeRegistryImpl();
        FluidVariant oil = FluidVariant.of(id("oil"));
        FluidVariant gas = FluidVariant.of(id("gas"));

        service.register(
            id("distill"),
            new DistillationRecipeDefinition(
                FluidIngredient.exact(oil, 10), FluidVolume.of(gas, 5), FluidVolume.empty(), 100
            ),
            new DefinitionProvenance("buildcraft", "code", 0)
        );
        assertEquals(100, service.findDistillation(oil, (fluid, tag) -> false)
            .orElseThrow().recipe().powerRequiredMicroMj());

        assertTrue(service.reloadData(List.of(new MachineRecipeRegistration(
            id("distill"),
            new DistillationRecipeDefinition(
                FluidIngredient.exact(oil, 10), FluidVolume.of(gas, 5), FluidVolume.empty(), 200
            ),
            new DefinitionProvenance("pack", "data/test/distill.json", 100)
        ))).published());
        assertEquals(200, service.findDistillation(oil, (fluid, tag) -> false)
            .orElseThrow().recipe().powerRequiredMicroMj());

        List<MachineRecipeRegistration> ambiguous = List.of(
            new MachineRecipeRegistration(id("x"), new DistillationRecipeDefinition(
                FluidIngredient.exact(oil, 1), FluidVolume.of(gas, 1), FluidVolume.empty(), 1),
                new DefinitionProvenance("a", "a.json", 10)),
            new MachineRecipeRegistration(id("x"), new DistillationRecipeDefinition(
                FluidIngredient.exact(oil, 1), FluidVolume.of(gas, 1), FluidVolume.empty(), 2),
                new DefinitionProvenance("b", "b.json", 10))
        );
        assertFalse(service.reloadData(ambiguous).published());
        assertEquals(200, service.findDistillation(oil, (fluid, tag) -> false)
            .orElseThrow().recipe().powerRequiredMicroMj());
    }

    @Test
    public void replaceAndRemoveCompatibilityOperationsPublishSnapshots() {
        MachineRecipeRegistryImpl service = new MachineRecipeRegistryImpl();
        FluidVariant oil = FluidVariant.of(id("oil"));
        FluidVariant gas = FluidVariant.of(id("gas"));
        var provenance = new DefinitionProvenance("legacy", "code", 0);

        service.replaceCode(id("legacy"), new DistillationRecipeDefinition(
            FluidIngredient.exact(oil, 1), FluidVolume.of(gas, 1), FluidVolume.empty(), 1), provenance);
        service.replaceCode(id("legacy"), new DistillationRecipeDefinition(
            FluidIngredient.exact(oil, 1), FluidVolume.of(gas, 1), FluidVolume.empty(), 2), provenance);
        assertEquals(2, service.findDistillation(oil, (fluid, tag) -> false).orElseThrow().recipe().powerRequiredMicroMj());
        assertTrue(service.removeCode(id("legacy")));
        assertTrue(service.findDistillation(oil, (fluid, tag) -> false).isEmpty());
    }

    private static ResourceLocation id(String path) {
        return Objects.requireNonNull(ResourceLocation.tryParse("test:" + path));
    }
}
