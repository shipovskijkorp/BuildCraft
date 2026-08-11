package buildcraft.lib.internal.api.v2.persistence;

import buildcraft.api.v2.persistence.*;

import buildcraft.api.v2.registry.RegistrationContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PersistenceRegistryTester {
    @Test
    public void migratesAliasChainToCurrentSchema() {
        ApiCodec<String, List<String>> codec = new ApiCodec<>() {
            @Override
            public CodecResult<String> decode(List<String> payload) {
                return CodecResult.success(String.join("/", payload));
            }

            @Override
            public CodecResult<List<String>> encode(String value) {
                return CodecResult.success(new ArrayList<>(List.of(value)));
            }
        };

        PersistentType<String, List<String>> type = PersistentType
            .builder(id("canonical"), 2, codec)
            .alias(id("old"))
            .migration(appendMigration(0, 1, "v1"))
            .migration(appendMigration(1, 2, "v2"))
            .build();

        PersistenceRegistryBuilder<String, List<String>> builder = new PersistenceRegistryBuilder<>();
        RegistrationContext context = () -> "test-addon";
        builder.register(type, context);
        builder.alias(id("very_old"), id("old"), context);
        PersistenceRegistrySnapshot<String, List<String>> snapshot = builder.freeze();

        assertEquals(id("canonical"), snapshot.canonicalId(id("very_old")).orElseThrow());

        PayloadResolution<String, List<String>> result = snapshot.decode(
            new EncodedPayload<>(id("very_old"), 0, new ArrayList<>(List.of("base"))),
            ArrayList::new
        );

        assertTrue(result.known());
        KnownPayload<String, List<String>> known = (KnownPayload<String, List<String>>) result;
        assertEquals("base/v1/v2", known.value());
        assertEquals(0, known.storedSchemaVersion());
        assertEquals(2, known.currentSchemaVersion());
        assertEquals(id("canonical"), known.canonicalTypeId());
    }

    @Test
    public void preservesUnknownPayloadWithoutAliasingMutableInput() {
        PersistenceRegistrySnapshot<String, List<String>> snapshot =
            new PersistenceRegistryBuilder<String, List<String>>().freeze();
        List<String> raw = new ArrayList<>(List.of("opaque"));

        PayloadResolution<String, List<String>> result = snapshot.decode(
            new EncodedPayload<>(id("missing"), 7, raw),
            ArrayList::new
        );

        assertFalse(result.known());
        UnknownPayload<String, List<String>> unknown = (UnknownPayload<String, List<String>>) result;
        raw.add("changed-after-decode");

        assertEquals(UnknownPayloadReason.MISSING_TYPE, unknown.reason());
        assertEquals(List.of("opaque"), unknown.rawPayload());

        EncodedPayload<List<String>> encoded = unknown.toEncodedPayload(ArrayList::new);
        assertEquals(id("missing"), encoded.typeId());
        assertEquals(7, encoded.schemaVersion());
        assertEquals(List.of("opaque"), encoded.payload());
    }

    @Test
    public void rejectsAliasCycles() {
        PersistenceRegistryBuilder<String, String> builder = new PersistenceRegistryBuilder<>();
        RegistrationContext context = () -> "test-addon";
        builder.alias(id("a"), id("b"), context);
        builder.alias(id("b"), id("a"), context);

        assertThrows(IllegalStateException.class, builder::freeze);
    }

    @Test
    public void keepsOriginalPayloadWhenMigrationFails() {
        ApiCodec<String, List<String>> codec = new ApiCodec<>() {
            @Override
            public CodecResult<String> decode(List<String> payload) {
                return CodecResult.success(String.join("/", payload));
            }

            @Override
            public CodecResult<List<String>> encode(String value) {
                return CodecResult.success(new ArrayList<>(List.of(value)));
            }
        };
        PersistentType<String, List<String>> type = PersistentType
            .builder(id("type"), 1, codec)
            .migration(new SchemaMigration<>() {
                @Override
                public int fromVersion() {
                    return 0;
                }

                @Override
                public int toVersion() {
                    return 1;
                }

                @Override
                public CodecResult<List<String>> migrate(List<String> payload) {
                    payload.add("partially-mutated");
                    return CodecResult.failure("intentional migration failure");
                }
            })
            .build();

        PersistenceRegistryBuilder<String, List<String>> builder = new PersistenceRegistryBuilder<>();
        builder.register(type, () -> "test-addon");
        PayloadResolution<String, List<String>> result = builder.freeze().decode(
            new EncodedPayload<>(id("type"), 0, new ArrayList<>(List.of("original"))),
            ArrayList::new
        );

        UnknownPayload<String, List<String>> unknown = (UnknownPayload<String, List<String>>) result;
        assertEquals(UnknownPayloadReason.MIGRATION_FAILED, unknown.reason());
        assertEquals(List.of("original"), unknown.rawPayload());
    }

    private static SchemaMigration<List<String>> appendMigration(int from, int to, String value) {
        return new SchemaMigration<>() {
            @Override
            public int fromVersion() {
                return from;
            }

            @Override
            public int toVersion() {
                return to;
            }

            @Override
            public CodecResult<List<String>> migrate(List<String> payload) {
                payload.add(value);
                return CodecResult.success(payload);
            }
        };
    }

    private static ResourceLocation id(String path) {
        return Objects.requireNonNull(ResourceLocation.tryParse("test:" + path));
    }
}
