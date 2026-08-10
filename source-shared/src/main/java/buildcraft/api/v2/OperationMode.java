package buildcraft.api.v2;

/**
 * Common simulation semantics for all transfer APIs.
 */
public enum OperationMode {
    SIMULATE,
    EXECUTE;

    public boolean isSimulation() {
        return this == SIMULATE;
    }
}
