package buildcraft.api.v2.diagnostics;

public record ApiDiagnostic(Level level, String message) {
    public enum Level { INFO, WARNING, ERROR }
}
