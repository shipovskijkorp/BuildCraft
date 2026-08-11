package buildcraft.api.v2.statement;

@FunctionalInterface
public interface ActionExecutor {
    StatementResult execute(StatementContext context, StatementParameters parameters);
}
