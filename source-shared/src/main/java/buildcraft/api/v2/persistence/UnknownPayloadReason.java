package buildcraft.api.v2.persistence;

public enum UnknownPayloadReason {
    MISSING_TYPE,
    NEWER_SCHEMA,
    MISSING_MIGRATION,
    MIGRATION_FAILED,
    DECODE_FAILED
}
