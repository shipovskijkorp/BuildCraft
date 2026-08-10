package buildcraft.api.v2.reload;

import java.util.Objects;

/**
 * Origin and precedence of one reloadable definition.
 */
public record DefinitionProvenance(String owner, String source, int priority) {
    public DefinitionProvenance {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(source, "source");
        if (owner.isBlank()) {
            throw new IllegalArgumentException("owner must not be blank");
        }
        if (source.isBlank()) {
            throw new IllegalArgumentException("source must not be blank");
        }
    }
}
