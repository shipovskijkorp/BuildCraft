package buildcraft.api.v2.persistence;

/**
 * One deterministic forward schema migration.
 */
public interface SchemaMigration<P> {
    int fromVersion();

    int toVersion();

    CodecResult<P> migrate(P payload);
}
