package buildcraft.api.v2.persistence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * Stable persisted extension type: namespaced ID + schema + codec.
 */
public final class PersistentType<T, P> {
    private final ResourceLocation id;
    private final int schemaVersion;
    private final ApiCodec<T, P> codec;
    private final Set<ResourceLocation> aliases;
    private final List<SchemaMigration<P>> migrations;

    private PersistentType(
        ResourceLocation id,
        int schemaVersion,
        ApiCodec<T, P> codec,
        Set<ResourceLocation> aliases,
        List<SchemaMigration<P>> migrations
    ) {
        this.id = id;
        this.schemaVersion = schemaVersion;
        this.codec = codec;
        this.aliases = Collections.unmodifiableSet(new LinkedHashSet<>(aliases));
        this.migrations = Collections.unmodifiableList(new ArrayList<>(migrations));
    }

    public static <T, P> Builder<T, P> builder(ResourceLocation id, int schemaVersion, ApiCodec<T, P> codec) {
        return new Builder<>(id, schemaVersion, codec);
    }

    public ResourceLocation id() {
        return id;
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public ApiCodec<T, P> codec() {
        return codec;
    }

    public Set<ResourceLocation> aliases() {
        return aliases;
    }

    public List<SchemaMigration<P>> migrations() {
        return migrations;
    }

    public static final class Builder<T, P> {
        private final ResourceLocation id;
        private final int schemaVersion;
        private final ApiCodec<T, P> codec;
        private final Set<ResourceLocation> aliases = new LinkedHashSet<>();
        private final List<SchemaMigration<P>> migrations = new ArrayList<>();

        private Builder(ResourceLocation id, int schemaVersion, ApiCodec<T, P> codec) {
            this.id = Objects.requireNonNull(id, "id");
            if (schemaVersion < 0) {
                throw new IllegalArgumentException("schemaVersion must be non-negative");
            }
            this.schemaVersion = schemaVersion;
            this.codec = Objects.requireNonNull(codec, "codec");
        }

        public Builder<T, P> alias(ResourceLocation alias) {
            Objects.requireNonNull(alias, "alias");
            if (alias.equals(id)) {
                throw new IllegalArgumentException("Type alias must differ from canonical id: " + id);
            }
            aliases.add(alias);
            return this;
        }

        public Builder<T, P> migration(SchemaMigration<P> migration) {
            migrations.add(Objects.requireNonNull(migration, "migration"));
            return this;
        }

        public PersistentType<T, P> build() {
            validateMigrations();
            return new PersistentType<>(id, schemaVersion, codec, aliases, migrations);
        }

        private void validateMigrations() {
            Set<Integer> fromVersions = new LinkedHashSet<>();
            for (SchemaMigration<P> migration : migrations) {
                int from = migration.fromVersion();
                int to = migration.toVersion();
                if (from < 0) {
                    throw new IllegalArgumentException("Migration source version must be non-negative for " + id);
                }
                if (to <= from) {
                    throw new IllegalArgumentException("Migration must move forward for " + id + ": " + from + " -> " + to);
                }
                if (to > schemaVersion) {
                    throw new IllegalArgumentException(
                        "Migration for " + id + " targets schema " + to + " newer than current schema " + schemaVersion
                    );
                }
                if (!fromVersions.add(from)) {
                    throw new IllegalArgumentException("Ambiguous migrations from schema " + from + " for " + id);
                }
            }
        }
    }
}
