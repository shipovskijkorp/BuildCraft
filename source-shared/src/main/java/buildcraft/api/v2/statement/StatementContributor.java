package buildcraft.api.v2.statement;

@FunctionalInterface
public interface StatementContributor {
    void contribute(StatementContext context, StatementCollector collector);
}
