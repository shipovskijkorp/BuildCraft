package buildcraft.api.v2.gate;

import java.util.List;
import java.util.Objects;

public record GateProgram(List<GateRule> rules) {
    public GateProgram {
        rules = List.copyOf(Objects.requireNonNull(rules, "rules"));
    }
}
