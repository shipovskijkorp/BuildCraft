/*
 * Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.fluid;

import buildcraft.lib.misc.FluidStackUtil;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class SingleUseTank extends FluidTank {

    private static final String NBT_ACCEPTED_FLUID = "acceptedFluid";

    private FluidStack acceptedFluid;

    public SingleUseTank(int capacity) {
        super(capacity);
    }

    @Override
    public int fill(FluidStack resource, FluidAction doFill) {
        resource = FluidCompatRegistry.canonicalize(resource);
        if (resource.isEmpty()) {
            return 0;
        }

        if (doFill.execute() && acceptedFluid == null) {
            acceptedFluid = resource.copy();
            acceptedFluid.setAmount(1);
        }

        if (acceptedFluid == null || FluidCompatRegistry.areEquivalent(acceptedFluid, resource)) {
            return super.fill(resource, doFill);
        }

        return 0;
    }

    public void reset() {
        acceptedFluid = null;
    }

    public void setAcceptedFluid(Fluid fluid) {
        if (fluid == null) {
            this.acceptedFluid = null;
        } else {
            this.acceptedFluid = FluidCompatRegistry.canonicalize(new FluidStack(fluid, 1));
        }
    }

    public void setAcceptedFluid(FluidStack fluid) {
        if (fluid == null) {
            this.acceptedFluid = null;
        } else {
            this.acceptedFluid = FluidCompatRegistry.canonicalize(fluid.copyWithAmount(1));
        }
    }

    public FluidStack getAcceptedFluid() {
        return acceptedFluid;
    }

    @Override
    public FluidTank readFromNBT(HolderLookup.Provider lookupProvider, CompoundTag nbt) {
        FluidStack loaded = FluidCompatRegistry.canonicalize(
            FluidStackUtil.parseOptional(lookupProvider, nbt.getCompound(NBT_ACCEPTED_FLUID))
        );
        acceptedFluid = loaded.isEmpty() ? null : loaded.copyWithAmount(1);
        return super.readFromNBT(lookupProvider, FluidStackUtil.normalizeLegacyNbt(nbt));
    }

    @Override
    public CompoundTag writeToNBT(HolderLookup.Provider lookupProvider, CompoundTag nbt) {
        if (acceptedFluid != null) {
            nbt.put(NBT_ACCEPTED_FLUID, FluidStackUtil.saveOptional(acceptedFluid, lookupProvider));
        }
        return super.writeToNBT(lookupProvider, nbt);
    }

}
