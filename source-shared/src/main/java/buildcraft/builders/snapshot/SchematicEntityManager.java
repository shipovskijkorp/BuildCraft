/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 */
package buildcraft.builders.snapshot;

import buildcraft.lib.internal.core.InvalidInputDataException;
import buildcraft.api.v2.BuildCraftApi;
import buildcraft.api.v2.BuildCraftServices;
import buildcraft.api.v2.permission.AutomationActor;
import buildcraft.api.v2.schematic.SchematicEntityCaptureContext;
import buildcraft.api.v2.schematic.SnapshotElement;
import buildcraft.api.v2.schematic.UnknownSnapshotElement;
import buildcraft.builders.internal.schematic.api2.Api2SchematicEntity;
import buildcraft.builders.internal.schematic.api2.Api2SnapshotPersistence;
import buildcraft.builders.internal.schematic.api2.SchematicServiceImpl;
import buildcraft.builders.internal.schematic.api2.UnavailableSchematicAdapters;
import buildcraft.builders.internal.schematic.legacy.ISchematicEntity;
import buildcraft.builders.internal.schematic.legacy.SchematicEntityContext;
import buildcraft.builders.internal.schematic.legacy.SchematicEntityFactory;
import buildcraft.builders.internal.schematic.legacy.SchematicEntityFactoryRegistry;
import com.google.common.collect.Lists;
import javax.annotation.Nonnull;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** Internal compatibility manager. New entity capture enters through API2 SchematicService. */
public class SchematicEntityManager {
    public static ISchematicEntity getSchematicEntity(SchematicEntityContext context) {
        SnapshotElement element = BuildCraftApi.service(BuildCraftServices.SCHEMATICS)
            .captureEntity(new SchematicEntityCaptureContext(context.world, context.basePos, context.entity, AutomationActor.unknown()))
            .orElse(null);
        if (element == null) return null;
        if (element instanceof ISchematicEntity legacy) return legacy;
        return new Api2SchematicEntity(
            element,
            SchematicServiceImpl.INSTANCE.findEntityAdapter(element).orElse(UnavailableSchematicAdapters.ENTITY)
        );
    }

    /** Legacy built-in factory backend used only as API2 fallback. */
    public static ISchematicEntity getLegacySchematicEntity(SchematicEntityContext context) {
        for (SchematicEntityFactory<?> factory : Lists.reverse(SchematicEntityFactoryRegistry.getFactories())) {
            if (factory.predicate.test(context)) {
                ISchematicEntity schematicEntity = factory.supplier.get();
                schematicEntity.init(context);
                return schematicEntity;
            }
        }
        return null;
    }

    public static <S extends ISchematicEntity> S createCleanCopy(S schematicEntity) {
        return SchematicEntityFactoryRegistry.getFactoryByInstance(schematicEntity).supplier.get();
    }

    @Nonnull
    public static CompoundTag writeToNBT(ISchematicEntity schematicEntity) {
        if (schematicEntity instanceof UnavailableSchematicEntity unavailable) {
            return unavailable.serializedEnvelope();
        }
        CompoundTag tag = new CompoundTag();
        if (schematicEntity instanceof Api2SchematicEntity api2) {
            tag.putBoolean("api2", true);
            tag.put("data", Api2SnapshotPersistence.write(api2.element()));
            return tag;
        }
        tag.putString("name", SchematicEntityFactoryRegistry.getFactoryByInstance(schematicEntity).name.toString());
        tag.put("data", schematicEntity.serializeNBT());
        return tag;
    }

    @Nonnull
    public static ISchematicEntity readFromNBTAllowUnavailable(CompoundTag tag) throws InvalidInputDataException {
        if (!tag.getBoolean("api2")) {
            ResourceLocation name = ResourceLocation.tryParse(tag.getString("name"));
            if (name == null) {
                throw new InvalidInputDataException("Invalid schematic entity type id " + tag.getString("name"));
            }
            if (SchematicEntityFactoryRegistry.getFactoryByName(name) == null) {
                return new UnavailableSchematicEntity(tag);
            }
        }
        return readFromNBT(tag);
    }

    public static boolean isUnavailable(ISchematicEntity schematicEntity) {
        if (schematicEntity instanceof UnavailableSchematicEntity) {
            return true;
        }
        if (schematicEntity instanceof Api2SchematicEntity api2) {
            return api2.adapter() == UnavailableSchematicAdapters.ENTITY
                || api2.element() instanceof UnknownSnapshotElement;
        }
        return false;
    }

    @Nonnull
    public static ISchematicEntity readFromNBT(CompoundTag tag) throws InvalidInputDataException {
        if (tag.getBoolean("api2")) {
            try {
                SnapshotElement element = Api2SnapshotPersistence.read(tag.getCompound("data"));
                return new Api2SchematicEntity(
                    element,
                    SchematicServiceImpl.INSTANCE.findEntityAdapter(element).orElse(UnavailableSchematicAdapters.ENTITY)
                );
            } catch (RuntimeException e) {
                throw new InvalidInputDataException("Failed to load API2 schematic entity from " + tag, e);
            }
        }
        ResourceLocation name = ResourceLocation.tryParse(tag.getString("name"));
        if (name == null) throw new InvalidInputDataException("Invalid schematic entity type id " + tag.getString("name"));
        SchematicEntityFactory<?> factory = SchematicEntityFactoryRegistry.getFactoryByName(name);
        if (factory == null) throw new InvalidInputDataException("Unknown schematic type " + name);
        ISchematicEntity schematicEntity = factory.supplier.get();
        CompoundTag data = tag.getCompound("data");
        try {
            schematicEntity.deserializeNBT(data);
            return schematicEntity;
        } catch (InvalidInputDataException e) {
            throw new InvalidInputDataException("Failed to load the schematic from " + data, e);
        }
    }
}
