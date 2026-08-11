package buildcraft.api.v2;

/** Semantic version of the public BuildCraft extension API. */
public record ApiVersion(int major, int minor, int patch) implements Comparable<ApiVersion> {
    public ApiVersion {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("API version components must be non-negative");
        }
    }

    @Override
    public int compareTo(ApiVersion other) {
        int result = Integer.compare(major, other.major);
        if (result != 0) return result;
        result = Integer.compare(minor, other.minor);
        return result != 0 ? result : Integer.compare(patch, other.patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
