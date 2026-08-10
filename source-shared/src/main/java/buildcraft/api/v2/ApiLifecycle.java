package buildcraft.api.v2;

/**
 * Bootstrap phases. Runtime reload is handled independently.
 */
public enum ApiLifecycle {
    DISCOVERY,
    TYPE_REGISTRATION,
    CONTENT_REGISTRATION,
    FROZEN,
    RUNNING,
    STOPPING
}
