package buildcraft.api.v2.debug;

import java.util.List;
import java.util.Objects;

/** Immutable diagnostic section intended for developer tools, not localization-sensitive gameplay UI. */
public record DebugSection(String title, List<String> lines) {
    public DebugSection {
        title = Objects.requireNonNull(title, "title");
        lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
    }
}
