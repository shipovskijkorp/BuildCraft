package buildcraft.api.v2.statement;

@FunctionalInterface
public interface TriggerEvaluator {
    boolean evaluate(StatementContext context, StatementParameters parameters);
}
