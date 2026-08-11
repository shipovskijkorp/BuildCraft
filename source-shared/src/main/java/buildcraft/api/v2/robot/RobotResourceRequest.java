package buildcraft.api.v2.robot;

import java.util.Objects;

public record RobotResourceRequest(RobotResource resource, long amount) {
    public RobotResourceRequest {
        Objects.requireNonNull(resource, "resource");
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
    }
}
