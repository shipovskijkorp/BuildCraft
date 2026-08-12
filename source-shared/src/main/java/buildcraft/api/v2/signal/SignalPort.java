package buildcraft.api.v2.signal;

import buildcraft.api.v2.OperationMode;

public interface SignalPort<T> {
    SignalChannelType<T> channel();
    /** Whether this endpoint is currently attached to a physical/logical signal network. */
    default boolean connected() { return true; }
    T value();
    SignalUpdateResult<T> publish(T value, OperationMode mode);
}
