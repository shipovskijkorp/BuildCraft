/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 */
package buildcraft.builders.snapshot;

import buildcraft.api.core.InvalidInputDataException;
import buildcraft.api.v2.BuildCraftApi;
import buildcraft.api.v2.BuildCraftServices;
import buildcraft.api.v2.permission.AutomationActor;
import buildcraft.api.v2.schematic.SchematicCaptureContext;
import buildcraft.api.v2.schematic.SnapshotElement;
import buildcraft.builders.internal.schematic.api2.Api2SchematicBlock;
import buildcraft.builders.internal.schematic.api2.Api2SnapshotPersistence;
import buildcraft.builders.internal.schematic.api2.SchematicServiceImpl;
import buildcraft.builders.internal.schematic.api2.UnavailableSchematicAdapters;
import buildcraft.builders.internal.schematic.legacy.ISchematicBlock;
import buildcraft.builders.internal.schematic.legacy.SchematicBlockContext;
import buildcraft.builders.internal.schematic.legacy.SchematicBlockFactory;
import buildcraft.builders.internal.schematic.legacy.SchematicBlockFactoryRegistry;
import com.google.common.collect.Lists;
import javax.annotation.Nonnull;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** Internal compatibility manager. New capture enters through API2 SchematicService. */
public class SchematicBlockManager {
    public static ISchematicBlock getSchematicBlock(SchematicBlockContext context) {
        SnapshotElement element = BuildCraftApi.service(BuildCraftServices.SCHEMATICS)
            .capture(new SchematicCaptureContext(
                context.world, context.basePos, context.pos, context.blockState, AutomationActor.unknown()
            ))
            .orElseThrow(() -> new UnsupportedOperationException("No schematic adapter for " + context.blockState));
        if (element instanceof ISchematicBlock legacy) return legacy;
        return new Api2SchematicBlock(
            element,
            SchematicServiceImpl.INSTANCE.findBlockAdapter(element).orElse(UnavailableSchematicAdapters.BLOCK)
        );
    }

    /** Legacy built-in factory backend used only as API2 fallback. */
    public static ISchematicBlock getLegacySchematicBlock(SchematicBlockContext context) {
        for (SchematicBlockFactory<?> factory : Lists.reverse(SchematicBlockFactoryRegistry.getFactories())) {
            if (factory.predicate.test(context)) {
                ISchematicBlock schematicBlock = factory.supplier.get();
                schematicBlock.init(context);
                return schematicBlock;
            }
        }
        throw new UnsupportedOperationException("No built-in schematic factory for " + context.blockState);
    }

    public static <S extends ISchematicBlock> S createCleanCopy(S schematicBlock) {
        return SchematicBlockFactoryRegistry.getFactoryByInstance(schematicBlock).supplier.get();
    }

    @Nonnull
    public static CompoundTag writeToNBT(ISchematicBlock schematicBlock) {
        CompoundTag tag = new CompoundTag();
        if (schematicBlock instanceof Api2SchematicBlock api2) {
            tag.putBoolean("api2", true);
            tag.put("data", Api2SnapshotPersistence.write(api2.element()));
            return tag;
        }
        tag.putString("name", SchematicBlockFactoryRegistry.getFactoryByInstance(schematicBlock).name.toString());
        tag.put("data", schematicBlock.serializeNBT());
        return tag;
    }

    @Nonnull
    public static ISchematicBlock readFromNBT(CompoundTag tag) throws InvalidInputDataException {
        if (tag.getBoolean("api2")) {
            try {
                SnapshotElement element = Api2SnapshotPersistence.read(tag.getCompound("data"));
                return new Api2SchematicBlock(
                    element,
                    SchematicServiceImpl.INSTANCE.findBlockAdapter(element).orElse(UnavailableSchematicAdapters.BLOCK)
                );
            } catch (RuntimeException e) {
                throw new InvalidInputDataException("Failed to load API2 schematic block from " + tag, e);
            }
        }
        ResourceLocation name = ResourceLocation.tryParse(tag.getString("name"));
        if (name == null) throw new InvalidInputDataException("Invalid schematic type id " + tag.getString("name"));
        SchematicBlockFactory<?> factory = SchematicBlockFactoryRegistry.getFactoryByName(name);
        if (factory == null) throw new InvalidInputDataException("Unknown schematic type " + name);
        ISchematicBlock schematicBlock = factory.supplier.get();
        CompoundTag data = tag.getCompound("data");
        try {
            schematicBlock.deserializeNBT(data);
            return schematicBlock;
        } catch (InvalidInputDataException e) {
            throw new InvalidInputDataException("Failed to load the schematic from " + data, e);
        }
    }
}
