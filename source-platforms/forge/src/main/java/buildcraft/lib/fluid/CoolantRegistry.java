/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 */
package buildcraft.lib.fluid;

import buildcraft.api.fuels.ICoolant;
import buildcraft.api.fuels.ICoolantManager;
import buildcraft.api.fuels.ISolidCoolant;
import buildcraft.api.v2.BuildCraftApi;
import buildcraft.api.v2.BuildCraftServices;
import buildcraft.api.v2.fuels.CoolantProfile;
import buildcraft.api.v2.fuels.EnergyFluidService;
import buildcraft.api.v2.fuels.ProfileMatch;
import buildcraft.api.v2.fuels.SolidCoolantProfile;
import buildcraft.api.v2.reload.DefinitionProvenance;
import buildcraft.lib.api.v2.LegacyEnergyFluidIds;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/** Legacy ICoolantManager facade backed by API 2 definitions. */
public enum CoolantRegistry implements ICoolantManager {
    INSTANCE;

    private static final DefinitionProvenance LEGACY_PROVENANCE =
        new DefinitionProvenance("legacy-api", "BuildcraftFuelRegistry.coolant", -1000);

    private final Map<ResourceLocation, ICoolant> legacyCoolants = new LinkedHashMap<>();
    private final Map<ResourceLocation, ISolidCoolant> legacySolidCoolants = new LinkedHashMap<>();

    private EnergyFluidService service() {
        return BuildCraftApi.runtime().requireService(BuildCraftServices.ENERGY_FLUIDS);
    }

    @Override
    public synchronized ICoolant addCoolant(ICoolant coolant) {
        Objects.requireNonNull(coolant, "coolant");
        CoolantProfile profile = new CoolantProfile(
            (candidate, context) -> {
                FluidStack stack = FuelApiBridge.stackOfVariant(candidate, 1);
                return !stack.isEmpty() && coolant.matchesFluid(stack);
            },
            (candidate, heat) -> {
                FluidStack stack = FuelApiBridge.stackOfVariant(candidate, 1);
                if (stack.isEmpty()) return 0;
                return coolant.getDegreesCoolingPerMB(stack, (float) heat);
            }
        );
        ResourceLocation id = LegacyEnergyFluidIds.nextAnonymous("coolant");
        service().register(id, profile, LEGACY_PROVENANCE);
        legacyCoolants.put(id, coolant);
        return coolant;
    }

    @Override
    public synchronized ISolidCoolant addSolidCoolant(ISolidCoolant solidCoolant) {
        Objects.requireNonNull(solidCoolant, "solidCoolant");
        SolidCoolantProfile profile = new SolidCoolantProfile(
            stack -> {
                FluidStack result = solidCoolant.getFluidFromSolidCoolant(stack.copy());
                return result != null && !result.isEmpty() && result.getAmount() > 0;
            },
            stack -> FuelApiBridge.volumeOf(solidCoolant.getFluidFromSolidCoolant(stack.copy()))
        );
        ResourceLocation id = LegacyEnergyFluidIds.nextAnonymous("solid_coolant");
        service().register(id, profile, LEGACY_PROVENANCE);
        legacySolidCoolants.put(id, solidCoolant);
        return solidCoolant;
    }

    @Override
    public ICoolant addCoolant(FluidStack fluid, float degreesCoolingPerMB) {
        if (fluid == null || fluid.isEmpty()) throw new IllegalArgumentException("Coolant fluid must not be empty");
        return addCoolant(new Coolant(fluid, degreesCoolingPerMB));
    }

    @Override
    public ISolidCoolant addSolidCoolant(ItemStack solid, FluidStack fluid, float multiplier) {
        return addSolidCoolant(new SolidCoolant(solid, fluid, multiplier));
    }

    @Override
    public synchronized Collection<ICoolant> getCoolants() {
        ArrayList<ICoolant> result = new ArrayList<>();
        for (ProfileMatch<CoolantProfile> match : service().coolants()) {
            result.add(legacyCoolants.getOrDefault(match.id(), new V2CoolantView(match.profile())));
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public synchronized Collection<ISolidCoolant> getSolidCoolants() {
        ArrayList<ISolidCoolant> result = new ArrayList<>();
        for (ProfileMatch<SolidCoolantProfile> match : service().solidCoolants()) {
            result.add(legacySolidCoolants.getOrDefault(match.id(), new V2SolidCoolantView(match.profile())));
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public synchronized ICoolant getCoolant(FluidStack fluid) {
        if (fluid == null || fluid.isEmpty() || fluid.getAmount() <= 0) return null;
        ProfileMatch<CoolantProfile> match = service()
            .findCoolant(FuelApiBridge.variantOf(fluid), FuelApiBridge.MATCH_CONTEXT)
            .orElse(null);
        if (match == null) return null;
        return legacyCoolants.getOrDefault(match.id(), new V2CoolantView(match.profile()));
    }

    @Override
    public float getDegreesPerMb(FluidStack fluid, float heat) {
        if (fluid == null || fluid.isEmpty() || fluid.getAmount() <= 0) return 0;
        ProfileMatch<CoolantProfile> match = service()
            .findCoolant(FuelApiBridge.variantOf(fluid), FuelApiBridge.MATCH_CONTEXT)
            .orElse(null);
        if (match == null) return 0;
        return (float) match.profile().degreesPerMilliBucket(FuelApiBridge.variantOf(fluid), heat);
    }

    @Override
    public synchronized ISolidCoolant getSolidCoolant(ItemStack solid) {
        if (solid == null || solid.isEmpty()) return null;
        ProfileMatch<SolidCoolantProfile> match = service().findSolidCoolant(solid).orElse(null);
        if (match == null) return null;
        return legacySolidCoolants.getOrDefault(match.id(), new V2SolidCoolantView(match.profile()));
    }

    public static class Coolant implements ICoolant {
        private final FluidStack fluid;
        private final float degreesCoolingPerMB;

        public Coolant(FluidStack fluid, float degreesCoolingPerMB) {
            this.fluid = Objects.requireNonNull(fluid, "fluid").copy();
            if (!Float.isFinite(degreesCoolingPerMB) || degreesCoolingPerMB <= 0) {
                throw new IllegalArgumentException("degreesCoolingPerMB must be finite and > 0");
            }
            this.degreesCoolingPerMB = degreesCoolingPerMB;
        }

        @Override
        public boolean matchesFluid(FluidStack stack) {
            return FluidCompatRegistry.areEquivalent(fluid, stack);
        }

        @Override
        public float getDegreesCoolingPerMB(FluidStack stack, float heat) {
            return matchesFluid(stack) ? degreesCoolingPerMB : 0;
        }
    }

    private static class SolidCoolant implements ISolidCoolant {
        private final ItemStack solid;
        private final FluidStack fluid;
        private final float multiplier;

        private SolidCoolant(ItemStack solid, FluidStack fluid, float multiplier) {
            this.solid = Objects.requireNonNull(solid, "solid").copy();
            this.fluid = Objects.requireNonNull(fluid, "fluid").copy();
            if (this.solid.isEmpty()) throw new IllegalArgumentException("Solid coolant item must not be empty");
            if (this.fluid.isEmpty() || this.fluid.getAmount() <= 0) throw new IllegalArgumentException("Solid coolant fluid must not be empty");
            if (!Float.isFinite(multiplier) || multiplier <= 0) throw new IllegalArgumentException("multiplier must be finite and > 0");
            this.multiplier = multiplier;
        }

        @Override
        public FluidStack getFluidFromSolidCoolant(ItemStack stack) {
            //? if <1.20 {
            if (stack == null || !stack.sameItem(solid)) {
            //?} else {
            /*?
            if (stack == null || !ItemStack.isSameItem(stack, solid)) {
            ?*/
            //?}
                return null;
            }
            long amount = (long) (stack.getCount() * (double) fluid.getAmount() * multiplier / solid.getCount());
            if (amount <= 0) return null;
            if (amount > Integer.MAX_VALUE) throw new ArithmeticException("Solid coolant conversion overflow: " + amount + " mB");
            FluidStack result = fluid.copy();
            result.setAmount((int) amount);
            return result;
        }
    }

    private static final class V2CoolantView implements ICoolant {
        private final CoolantProfile profile;
        private V2CoolantView(CoolantProfile profile) { this.profile = profile; }
        @Override public boolean matchesFluid(FluidStack fluid) {
            return fluid != null && !fluid.isEmpty()
                && profile.matches(FuelApiBridge.variantOf(fluid), FuelApiBridge.MATCH_CONTEXT);
        }
        @Override public float getDegreesCoolingPerMB(FluidStack fluid, float heat) {
            if (!matchesFluid(fluid)) return 0;
            return (float) profile.degreesPerMilliBucket(FuelApiBridge.variantOf(fluid), heat);
        }
    }

    private static final class V2SolidCoolantView implements ISolidCoolant {
        private final SolidCoolantProfile profile;
        private V2SolidCoolantView(SolidCoolantProfile profile) { this.profile = profile; }
        @Override public FluidStack getFluidFromSolidCoolant(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return null;
            FluidStack result = FuelApiBridge.stackOf(profile.convert(stack));
            return result.isEmpty() ? null : result;
        }
    }
}
