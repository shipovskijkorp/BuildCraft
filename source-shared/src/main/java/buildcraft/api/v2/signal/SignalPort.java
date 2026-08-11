package buildcraft.api.v2.signal;

import buildcraft.api.v2.OperationMode;

public interface SignalPort<T> {
    SignalChannelType<T> channel();
    T value();
    SignalUpdateResult<T> publish(T value, OperationMode mode);
}
