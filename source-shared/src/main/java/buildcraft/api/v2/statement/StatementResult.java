package buildcraft.api.v2.statement;

import java.util.Objects;

public record StatementResult(Status status, String detail) {
    public enum Status { PASS, SUCCESS, DENIED, FAILED }
    public StatementResult {
        Objects.requireNonNull(status, "status");
        detail = detail == null ? "" : detail;
    }
    public static StatementResult success() { return new StatementResult(Status.SUCCESS, ""); }
    public static StatementResult pass() { return new StatementResult(Status.PASS, ""); }
}
