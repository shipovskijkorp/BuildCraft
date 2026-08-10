package dev.bcce.apifixture;

import buildcraft.api.v2.BuildCraftApi;
import buildcraft.api.v2.BuildCraftServices;
import buildcraft.api.v2.fuels.CoolantProfile;
import buildcraft.api.v2.fuels.EnergyFluidService;
import buildcraft.api.v2.fuels.FluidSelector;
import buildcraft.api.v2.fuels.FuelProfile;
import buildcraft.api.v2.reload.DefinitionProvenance;
import buildcraft.api.v2.permission.PermissionDecision;
import buildcraft.api.v2.permission.PermissionServiceRegistry;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * Compile-only consumer proving that a third-party addon can use the first API
 * 2 production domain without importing BuildCraft implementation or loader APIs.
 */
public final class ApiV2FixtureAddon {
    private ApiV2FixtureAddon() {}

    public static void registerExamples() {
        EnergyFluidService energyFluids = BuildCraftApi.runtime().requireService(BuildCraftServices.ENERGY_FLUIDS);
        PermissionServiceRegistry permissions = BuildCraftApi.runtime().requireService(BuildCraftServices.PERMISSIONS);
        DefinitionProvenance provenance = new DefinitionProvenance("api-v2-fixture", "fixture-code", 0);

        permissions.register(id("permission_provider"), 0, context -> PermissionDecision.pass());

        energyFluids.register(
            id("fixture_fuel"),
            FuelProfile.clean(FluidSelector.fluid(id("fixture_oil")), 2_000_000L, 12_000),
            provenance
        );
        energyFluids.register(
            id("fixture_coolant"),
            CoolantProfile.constant(FluidSelector.tag(id("fixture_coolants")), 0.003),
            provenance
        );
    }

    private static ResourceLocation id(String path) {
        return Objects.requireNonNull(ResourceLocation.tryParse("api_v2_fixture:" + path));
    }
}
