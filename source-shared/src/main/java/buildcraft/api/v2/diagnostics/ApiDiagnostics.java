package buildcraft.api.v2.diagnostics;

import java.util.List;
import java.util.Objects;

public interface ApiDiagnostics {
    List<ApiDiagnostic> entries();

    default void report(ApiDiagnostic diagnostic) {
        Objects.requireNonNull(diagnostic, "diagnostic");
        throw new UnsupportedOperationException("This diagnostics view is read-only");
    }
}
