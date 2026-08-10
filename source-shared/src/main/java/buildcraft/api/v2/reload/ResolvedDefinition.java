package buildcraft.api.v2.reload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * Winning definition plus the lower-precedence definitions it replaced.
 */
public final class ResolvedDefinition<V> {
    private final DefinitionEntry<V> winner;
    private final List<DefinitionEntry<V>> overridden;

    public ResolvedDefinition(DefinitionEntry<V> winner, List<DefinitionEntry<V>> overridden) {
        this.winner = Objects.requireNonNull(winner, "winner");
        this.overridden = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(overridden, "overridden")));
    }

    public ResourceLocation id() {
        return winner.id();
    }

    public V value() {
        return winner.value();
    }

    public DefinitionProvenance provenance() {
        return winner.provenance();
    }

    public DefinitionEntry<V> winner() {
        return winner;
    }

    public List<DefinitionEntry<V>> overridden() {
        return overridden;
    }
}
