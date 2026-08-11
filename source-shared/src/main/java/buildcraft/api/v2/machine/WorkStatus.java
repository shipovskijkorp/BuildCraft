package buildcraft.api.v2.machine;

import java.util.Objects;

public record WorkStatus(WorkState state, double progress, String detail) {
    public WorkStatus {
        Objects.requireNonNull(state, "state");
        if (!Double.isFinite(progress) || progress < 0.0 || progress > 1.0) throw new IllegalArgumentException("progress must be within [0,1]");
        detail = detail == null ? "" : detail;
    }
}
