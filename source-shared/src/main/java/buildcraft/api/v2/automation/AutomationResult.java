package buildcraft.api.v2.automation;

import java.util.Objects;

public record AutomationResult(Status status, long amount, String detail) {
    public enum Status { PASS, SUCCESS, DENIED, RETRY, FAILED }
    public AutomationResult {
        Objects.requireNonNull(status, "status");
        if (amount < 0) throw new IllegalArgumentException("amount must be non-negative");
        detail = detail == null ? "" : detail;
    }
    public static AutomationResult pass() { return new AutomationResult(Status.PASS, 0, ""); }
    public static AutomationResult success(long amount) { return new AutomationResult(Status.SUCCESS, amount, ""); }
}
