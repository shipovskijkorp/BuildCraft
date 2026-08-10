/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.snapshot;

import java.util.Optional;

import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.ForgeRegistries;

public class FluidStackRef {
    private final NbtRef<StringTag> fluid;
    private final NbtRef<IntTag> amount;

    public FluidStackRef(NbtRef<StringTag> fluid, NbtRef<IntTag> amount) {
        this.fluid = fluid;
        this.amount = amount;
    }

    public FluidStack get(Tag nbt) {
        String id = fluid.get(nbt)
            .map(StringTag::getAsString)
            .orElseThrow(() -> new IllegalArgumentException("Missing fluid registry ID for " + fluid));
        ResourceLocation key = parseRegistryId("fluid", id);
        if (!ForgeRegistries.FLUIDS.containsKey(key)) {
            throw new IllegalArgumentException("Unknown fluid registry ID '" + key + "'");
        }
        Fluid value = ForgeRegistries.FLUIDS.getValue(key);
        if (value == null) {
            throw new IllegalArgumentException("Registry returned no fluid for ID '" + key + "'");
        }
        int resolvedAmount = Optional.ofNullable(amount)
            .flatMap(ref -> ref.get(nbt))
            .map(IntTag::getAsInt)
            .orElse(FluidType.BUCKET_VOLUME);
        if (resolvedAmount < 0) {
            throw new IllegalArgumentException("Negative fluid amount " + resolvedAmount + " for registry ID '" + key + "'");
        }
        return new FluidStack(value, resolvedAmount);
    }

    private static ResourceLocation parseRegistryId(String type, String id) {
        try {
            return new ResourceLocation(id);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid " + type + " registry ID '" + id + "'", e);
        }
    }
}
