package buildcraft.api.v2.schematic;

import java.util.Objects;

public record SchematicResult(Status status, String detail) {
    public enum Status { PASS, SUCCESS, MISSING_RESOURCE, DENIED, FAILED }
    public SchematicResult {
        Objects.requireNonNull(status, "status"); detail = detail == null ? "" : detail;
    }
}
