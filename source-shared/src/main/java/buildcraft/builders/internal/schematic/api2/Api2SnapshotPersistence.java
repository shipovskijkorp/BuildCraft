package buildcraft.builders.internal.schematic.api2;

import buildcraft.api.v2.BuildCraftApi;
import buildcraft.api.v2.BuildCraftRegistries;
import buildcraft.api.v2.persistence.CodecResult;
import buildcraft.api.v2.persistence.OpaqueData;
import buildcraft.api.v2.persistence.SchemaMigration;
import buildcraft.api.v2.schematic.SnapshotElement;
import buildcraft.api.v2.schematic.SnapshotElementType;
import buildcraft.api.v2.schematic.UnknownSnapshotElement;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** Internal NBT envelope for addon-owned API2 snapshot elements. */
public final class Api2SnapshotPersistence {
    private static final String TYPE = "type";
    private static final String SCHEMA = "schema";
    private static final String FORMAT = "format";
    private static final String PAYLOAD = "payload";

    private Api2SnapshotPersistence() {}

    public static CompoundTag write(SnapshotElement element) {
        Objects.requireNonNull(element, "element");
        ResourceLocation typeId = element.typeId();
        int schema;
        OpaqueData payload;
        if (element instanceof UnknownSnapshotElement unknown) {
            schema = unknown.schemaVersion();
            payload = unknown.payload();
        } else {
            SnapshotElementType<?> rawType = BuildCraftApi.registry(BuildCraftRegistries.SNAPSHOT_ELEMENT_TYPES).get(typeId);
            if (rawType == null) {
                throw new IllegalStateException("No SnapshotElementType registered for " + typeId);
            }
            schema = rawType.persistence().schemaVersion();
            payload = encode(rawType, element).valueOrThrow();
        }
        CompoundTag tag = new CompoundTag();
        tag.putString(TYPE, typeId.toString());
        tag.putInt(SCHEMA, schema);
        tag.putString(FORMAT, payload.format().toString());
        tag.putByteArray(PAYLOAD, payload.bytes());
        return tag;
    }

    public static SnapshotElement read(CompoundTag tag) {
        ResourceLocation typeId = ResourceLocation.tryParse(tag.getString(TYPE));
        ResourceLocation format = ResourceLocation.tryParse(tag.getString(FORMAT));
        if (typeId == null || format == null) {
            throw new IllegalArgumentException("Invalid API2 snapshot envelope: " + tag);
        }
        int schema = Math.max(0, tag.getInt(SCHEMA));
        OpaqueData payload = new OpaqueData(format, tag.getByteArray(PAYLOAD));
        SnapshotElementType<?> rawType = BuildCraftApi.registry(BuildCraftRegistries.SNAPSHOT_ELEMENT_TYPES).get(typeId);
        if (rawType == null) {
            return new UnknownSnapshotElement(typeId, schema, payload);
        }
        int currentSchema = rawType.persistence().schemaVersion();
        OpaqueData migrated = payload;
        int migratedSchema = schema;
        while (migratedSchema < currentSchema) {
            SchemaMigration<OpaqueData> migration = findMigration(rawType, migratedSchema);
            if (migration == null) return new UnknownSnapshotElement(typeId, schema, payload);
            CodecResult<OpaqueData> result = migration.migrate(migrated);
            if (!result.successful()) return new UnknownSnapshotElement(typeId, schema, payload);
            migrated = result.valueOrThrow();
            migratedSchema = migration.toVersion();
        }
        if (migratedSchema != currentSchema) return new UnknownSnapshotElement(typeId, schema, payload);
        CodecResult<? extends SnapshotElement> decoded = decode(rawType, migrated);
        return decoded.successful()
            ? decoded.valueOrThrow()
            : new UnknownSnapshotElement(typeId, schema, payload);
    }


    @SuppressWarnings({"rawtypes", "unchecked"})
    private static SchemaMigration<OpaqueData> findMigration(SnapshotElementType rawType, int fromVersion) {
        for (Object value : rawType.persistence().migrations()) {
            SchemaMigration<OpaqueData> migration = (SchemaMigration<OpaqueData>) value;
            if (migration.fromVersion() == fromVersion) return migration;
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static CodecResult<OpaqueData> encode(SnapshotElementType rawType, SnapshotElement element) {
        return rawType.persistence().codec().encode(element);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static CodecResult<? extends SnapshotElement> decode(SnapshotElementType rawType, OpaqueData payload) {
        return rawType.persistence().codec().decode(payload);
    }
}
