package buildcraft.api.v2.statement;

import java.util.List;
import java.util.Objects;

public record ParameterSchema(List<ParameterSpec> parameters) {
    public static final ParameterSchema EMPTY = new ParameterSchema(List.of());
    public ParameterSchema {
        parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters"));
    }
}
