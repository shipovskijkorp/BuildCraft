package buildcraft.api.v2.energy;

/** Immutable BuildCraft power amount in micro-MJ. One MJ is 1,000,000 units. */
public record MjAmount(long microMj) implements Comparable<MjAmount> {
    public static final long MICRO_MJ_PER_MJ = 1_000_000L;
    public static final MjAmount ZERO = new MjAmount(0);

    public MjAmount {
        if (microMj < 0) throw new IllegalArgumentException("microMj must be non-negative");
    }

    public static MjAmount ofMicro(long value) { return value == 0 ? ZERO : new MjAmount(value); }
    public static MjAmount ofMj(long value) { return ofMicro(Math.multiplyExact(value, MICRO_MJ_PER_MJ)); }
    public boolean isZero() { return microMj == 0; }
    public MjAmount plus(MjAmount other) { return ofMicro(Math.addExact(microMj, other.microMj)); }
    public MjAmount minus(MjAmount other) {
        if (other.microMj > microMj) throw new ArithmeticException("negative MJ amount");
        return ofMicro(microMj - other.microMj);
    }
    @Override public int compareTo(MjAmount other) { return Long.compare(microMj, other.microMj); }
}
