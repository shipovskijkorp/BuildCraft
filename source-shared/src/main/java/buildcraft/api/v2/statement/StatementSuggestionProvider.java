package buildcraft.api.v2.statement;

import java.util.List;

@FunctionalInterface
public interface StatementSuggestionProvider<T> {
    List<T> suggestions(StatementContext context);
}
