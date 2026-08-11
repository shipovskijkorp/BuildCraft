package buildcraft.api.v2.robot;

public enum RobotEventDecision {
    PASS,
    ALLOW,
    DENY;

    /**
     * Combines independent listener decisions deterministically.
     * DENY is terminal, ALLOW wins over PASS, and PASS leaves the decision unchanged.
     */
    public RobotEventDecision merge(RobotEventDecision other) {
        if (this == DENY || other == DENY) return DENY;
        if (this == ALLOW || other == ALLOW) return ALLOW;
        return PASS;
    }

    public boolean isTerminal() {
        return this == DENY;
    }
}
