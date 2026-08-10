package buildcraft.api.v2.reload;

import java.util.List;

@FunctionalInterface
public interface DefinitionValidator<V> {
    List<ReloadDiagnostic> validate(DefinitionEntry<V> definition);
}
