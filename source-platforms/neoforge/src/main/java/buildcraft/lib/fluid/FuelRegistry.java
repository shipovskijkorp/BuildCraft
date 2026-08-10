/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 */
package buildcraft.lib.fluid;

import buildcraft.api.fuels.IFuel;
import buildcraft.api.fuels.IFuelManager;
import buildcraft.api.v2.BuildCraftApi;
import buildcraft.api.v2.BuildCraftServices;
import buildcraft.api.v2.fuels.EnergyFluidService;
import buildcraft.api.v2.fuels.FuelProfile;
import buildcraft.api.v2.fuels.ProfileMatch;
import buildcraft.api.v2.reload.DefinitionProvenance;
import buildcraft.lib.api.v2.LegacyEnergyFluidIds;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;

/** Legacy IFuelManager facade backed by the authoritative API 2 definition registry. */
public enum FuelRegistry implements IFuelManager {
    INSTANCE;

    private static final DefinitionProvenance LEGACY_PROVENANCE =
        new DefinitionProvenance("legacy-api", "BuildcraftFuelRegistry.fuel", -1000);

    private final Map<ResourceLocation, IFuel> legacyViews = new LinkedHashMap<>();

    private EnergyFluidService service() {
        return BuildCraftApi.runtime().requireService(BuildCraftServices.ENERGY_FLUIDS);
    }

    @Override
    public synchronized <F extends IFuel> F addFuel(F fuel) {
        Objects.requireNonNull(fuel, "fuel");
        FluidStack template = Objects.requireNonNull(fuel.getFluid(), "fuel.getFluid()");
        if (template.isEmpty()) throw new IllegalArgumentException("Fuel fluid must not be empty");

        FuelProfile profile;
        if (fuel instanceof IDirtyFuel dirtyFuel) {
            FluidStack residue = Objects.requireNonNull(dirtyFuel.getResidue(), "dirtyFuel.getResidue()");
            profile = FuelProfile.dirty(
                (candidate, context) -> FuelApiBridge.equivalentTo(template, candidate),
                fuel.getPowerPerCycle(), fuel.getTotalBurningTime(), FuelApiBridge.volumeOf(residue)
            );
        } else {
            profile = FuelProfile.clean(
                (candidate, context) -> FuelApiBridge.equivalentTo(template, candidate),
                fuel.getPowerPerCycle(), fuel.getTotalBurningTime()
            );
        }
        ResourceLocation id = LegacyEnergyFluidIds.next("fuel", FuelApiBridge.variantOf(template).fluidId());
        service().register(id, profile, LEGACY_PROVENANCE);
        legacyViews.put(id, fuel);
        return fuel;
    }

    @Override
    public IFuel addFuel(FluidStack fluid, long powerPerCycle, int totalBurningTime) {
        return addFuel(new Fuel(fluid, powerPerCycle, totalBurningTime));
    }

    @Override
    public IDirtyFuel addDirtyFuel(FluidStack fuel, long powerPerCycle, int totalBurningTime, FluidStack residue) {
        return addFuel(new DirtyFuel(fuel, powerPerCycle, totalBurningTime, residue));
    }

    @Override
    public synchronized Collection<IFuel> getFuels() {
        ArrayList<IFuel> result = new ArrayList<>();
        for (ProfileMatch<FuelProfile> match : service().fuels()) {
            IFuel legacy = legacyViews.get(match.id());
            if (legacy != null) {
                result.add(legacy);
                continue;
            }
            match.profile().representativeVariant().ifPresent(variant -> {
                FluidStack representative = FuelApiBridge.stackOfVariant(variant, 1);
                if (!representative.isEmpty()) result.add(view(match.profile(), representative));
            });
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public synchronized IFuel getFuel(FluidStack fluid) {
        if (fluid == null || fluid.isEmpty()) return null;
        ProfileMatch<FuelProfile> match = service()
            .findFuel(FuelApiBridge.variantOf(fluid), FuelApiBridge.MATCH_CONTEXT)
            .orElse(null);
        if (match == null) return null;
        IFuel legacy = legacyViews.get(match.id());
        return legacy != null ? legacy : view(match.profile(), fluid);
    }

    private static IFuel view(FuelProfile profile, FluidStack requestedFluid) {
        return profile.hasResidue()
            ? new V2DirtyFuelView(profile, requestedFluid)
            : new V2FuelView(profile, requestedFluid);
    }

    public static class Fuel implements IFuel {
        private final FluidStack fluid;
        private final long powerPerCycle;
        private final int totalBurningTime;

        public Fuel(FluidStack fluid, long powerPerCycle, int totalBurningTime) {
            this.fluid = Objects.requireNonNull(fluid, "fluid").copy();
            this.powerPerCycle = powerPerCycle;
            this.totalBurningTime = totalBurningTime;
        }

        @Override public FluidStack getFluid() { return fluid.copy(); }
        @Override public long getPowerPerCycle() { return powerPerCycle; }
        @Override public int getTotalBurningTime() { return totalBurningTime; }
    }

    public static class DirtyFuel extends Fuel implements IDirtyFuel {
        private final FluidStack residue;
        public DirtyFuel(FluidStack fluid, long powerPerCycle, int totalBurningTime, FluidStack residue) {
            super(fluid, powerPerCycle, totalBurningTime);
            this.residue = Objects.requireNonNull(residue, "residue").copy();
        }
        @Override public FluidStack getResidue() { return residue.copy(); }
    }

    private static class V2FuelView implements IFuel {
        protected final FuelProfile profile;
        private final FluidStack fluid;
        private V2FuelView(FuelProfile profile, FluidStack fluid) {
            this.profile = profile;
            this.fluid = fluid.copy();
            this.fluid.setAmount(1);
        }
        @Override public FluidStack getFluid() { return fluid.copy(); }
        @Override public int getTotalBurningTime() { return profile.burnTicksPerBucket(); }
        @Override public long getPowerPerCycle() { return profile.powerPerTickMicroMj(); }
    }

    private static final class V2DirtyFuelView extends V2FuelView implements IDirtyFuel {
        private V2DirtyFuelView(FuelProfile profile, FluidStack fluid) { super(profile, fluid); }
        @Override public FluidStack getResidue() { return FuelApiBridge.stackOf(profile.residuePerBucket()); }
    }
}
