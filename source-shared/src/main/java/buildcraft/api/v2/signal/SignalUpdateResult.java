package buildcraft.api.v2.signal;

import java.util.Objects;

public record SignalUpdateResult<T>(boolean changed, T previousValue, T currentValue) {
    public SignalUpdateResult {
        Objects.requireNonNull(previousValue, "previousValue");
        Objects.requireNonNull(currentValue, "currentValue");
    }
}
