package buildcraft.api.v2.gate;

import buildcraft.api.v2.statement.StatementSlot;
import java.util.Objects;

public record GateRule(StatementSlot trigger, StatementSlot action) {
    public GateRule {
        Objects.requireNonNull(trigger, "trigger");
        Objects.requireNonNull(action, "action");
    }
}
