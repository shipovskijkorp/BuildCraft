/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 */
package buildcraft.builders.snapshot;

import java.util.Objects;

import buildcraft.builders.internal.schematic.legacy.ISchematicBlock;
import buildcraft.builders.internal.schematic.legacy.SchematicBlockContext;
import buildcraft.lib.internal.core.InvalidInputDataException;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

/**
 * Lossless placeholder for a legacy schematic block whose owning addon is not currently installed.
 *
 * <p>The full serialized envelope is retained verbatim so saving the blueprint does not destroy data that can become
 * usable again when the addon returns. Unknown positions are treated as already built: Builder therefore leaves the
 * world untouched instead of excavating or replacing a block it cannot understand.</p>
 */
public final class UnavailableSchematicBlock implements ISchematicBlock {
    private static final ResourceLocation FALLBACK_TYPE = Objects.requireNonNull(
        ResourceLocation.tryParse("buildcraft:unavailable_schematic")
    );

    private final CompoundTag serialized;
    private final ResourceLocation typeId;

    public UnavailableSchematicBlock(CompoundTag serialized) {
        this.serialized = Objects.requireNonNull(serialized, "serialized").copy();
        ResourceLocation parsed = ResourceLocation.tryParse(serialized.getString("name"));
        this.typeId = parsed == null ? FALLBACK_TYPE : parsed;
    }

    public CompoundTag serializedEnvelope() {
        return serialized.copy();
    }

    @Override
    public ResourceLocation typeId() {
        return typeId;
    }

    @Override
    public void init(SchematicBlockContext context) {
    }

    @Override
    public ISchematicBlock getRotated(Rotation rotation) {
        // The absent addon owns the payload and therefore also owns its rotation semantics.
        return this;
    }

    @Override
    public boolean canBuild(Level world, BlockPos blockPos) {
        return false;
    }

    @Override
    public boolean build(Level world, BlockPos blockPos) {
        return false;
    }

    @Override
    public boolean buildWithoutChecks(Level world, BlockPos blockPos) {
        return false;
    }

    @Override
    public boolean isBuilt(Level world, BlockPos blockPos) {
        // Skip this position without mutating the world until the owning addon becomes available again.
        return true;
    }

    @Override
    public CompoundTag serializeNBT() {
        return serialized.getCompound("data").copy();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) throws InvalidInputDataException {
        throw new InvalidInputDataException("Unavailable schematic placeholders are immutable");
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof UnavailableSchematicBlock other && serialized.equals(other.serialized);
    }

    @Override
    public int hashCode() {
        return serialized.hashCode();
    }
}
