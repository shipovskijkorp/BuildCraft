package buildcraft.api.v2.signal;

@FunctionalInterface
public interface SignalCombiner<T> {
    T combine(T current, T incoming);
}
