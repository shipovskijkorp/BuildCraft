package buildcraft.api.v2.gate;

import buildcraft.api.v2.statement.StatementSlot;
import java.util.Optional;

/** One physical gate row. Either side may be empty while the player is editing the program. */
public record GateRule(StatementSlot trigger, StatementSlot action) {
    public Optional<StatementSlot> optionalTrigger() { return Optional.ofNullable(trigger); }
    public Optional<StatementSlot> optionalAction() { return Optional.ofNullable(action); }
    public boolean empty() { return trigger == null && action == null; }
}
