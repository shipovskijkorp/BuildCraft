package buildcraft.api.v2.gate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Complete gate program. Rules preserve physical gate slot order; connections[i] links
 * rule i to rule i+1 into one AND/OR group according to the gate's own logic variant.
 */
public record GateProgram(List<GateRule> rules, List<Boolean> connections) {
    public GateProgram {
        rules = List.copyOf(Objects.requireNonNull(rules, "rules"));
        connections = List.copyOf(Objects.requireNonNull(connections, "connections"));
        if (connections.size() > Math.max(0, rules.size() - 1)) {
            throw new IllegalArgumentException("A gate program cannot have more connections than rule gaps");
        }
        for (Boolean connection : connections) Objects.requireNonNull(connection, "connection");
    }

    public GateProgram(List<GateRule> rules) {
        this(rules, disconnected(rules == null ? 0 : rules.size()));
    }

    public boolean connectedAfter(int ruleIndex) {
        return ruleIndex >= 0 && ruleIndex < connections.size() && connections.get(ruleIndex);
    }

    private static List<Boolean> disconnected(int ruleCount) {
        List<Boolean> values = new ArrayList<>(Math.max(0, ruleCount - 1));
        for (int i = 1; i < ruleCount; i++) values.add(false);
        return values;
    }
}
