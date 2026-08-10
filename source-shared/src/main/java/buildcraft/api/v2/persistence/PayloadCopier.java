package buildcraft.api.v2.persistence;

/**
 * Defensive copy operation for serialized payloads.
 *
 * Unknown payload preservation depends on callers supplying a copier that
 * does not alias mutable input data.
 */
@FunctionalInterface
public interface PayloadCopier<P> {
    P copy(P payload);

    static <P> PayloadCopier<P> immutable() {
        return payload -> payload;
    }
}
