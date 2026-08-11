package buildcraft.api.v2.robot;

import java.util.Objects;

public record RobotTaskResult(Status status, String detail) {
    public enum Status { RUNNING, COMPLETE, RETRY, FAILED }
    public RobotTaskResult {
        Objects.requireNonNull(status, "status"); detail = detail == null ? "" : detail;
    }
    public static RobotTaskResult running() { return new RobotTaskResult(Status.RUNNING, ""); }
    public static RobotTaskResult complete() { return new RobotTaskResult(Status.COMPLETE, ""); }
}
