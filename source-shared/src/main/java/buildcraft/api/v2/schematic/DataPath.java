package buildcraft.api.v2.schematic;

import java.util.List;
import java.util.Objects;

public record DataPath(List<String> segments) {
    public DataPath {
        segments = List.copyOf(Objects.requireNonNull(segments, "segments"));
        if (segments.isEmpty() || segments.stream().anyMatch(segment -> segment == null || segment.isBlank())) {
            throw new IllegalArgumentException("DataPath must contain non-blank segments");
        }
    }
    public static DataPath of(String... segments) { return new DataPath(List.of(segments)); }
}
