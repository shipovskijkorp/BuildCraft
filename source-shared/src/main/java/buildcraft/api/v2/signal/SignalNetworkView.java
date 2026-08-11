package buildcraft.api.v2.signal;

import java.util.Collection;

public interface SignalNetworkView<T> {
    SignalChannelType<T> channel();
    T value();
    Collection<SignalEndpoint<T>> endpoints();
}
