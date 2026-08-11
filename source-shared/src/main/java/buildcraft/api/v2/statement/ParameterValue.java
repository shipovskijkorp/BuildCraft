package buildcraft.api.v2.statement;

import java.util.Objects;

public record ParameterValue<T>(ParameterType<T> type, T value) {
    public ParameterValue {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(value, "value");
    }
}
