package buildcraft.api.v2.signal;

import buildcraft.api.v2.OperationMode;

public interface SignalPort<T> {
    SignalChannelType<T> channel();
    /** Whether this endpoint is currently attached to a physical/logical signal network. */
    default boolean connected() { return true; }

    /** Current value observed from the attached signal network. */
    T value();

    /** Publishes this endpoint's local/source value into the attached signal network. */
    SignalUpdateResult<T> publish(T value, OperationMode mode);

    /**
     * Returns the local value currently being published by this endpoint.
     * Providers that can drive a BuildCraft wire should override this separately from {@link #value()}.
     */
    default T publishedValue() { return channel().defaultValue(); }

    /**
     * Delivers the combined value of the attached BuildCraft wire to an external endpoint.
     * Read-only/source-only endpoints may keep the default no-op implementation.
     */
    default SignalUpdateResult<T> receive(T value, OperationMode mode) {
        T previous = value();
        return new SignalUpdateResult<>(false, previous, previous);
    }
}
