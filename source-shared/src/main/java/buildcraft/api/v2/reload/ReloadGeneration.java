package buildcraft.api.v2.reload;

/**
 * Monotonic identifier for one reload attempt.
 */
public record ReloadGeneration(long id) implements Comparable<ReloadGeneration> {
    public ReloadGeneration {
        if (id < 0) {
            throw new IllegalArgumentException("Reload generation must be non-negative");
        }
    }

    @Override
    public int compareTo(ReloadGeneration other) {
        return Long.compare(id, other.id);
    }
}
