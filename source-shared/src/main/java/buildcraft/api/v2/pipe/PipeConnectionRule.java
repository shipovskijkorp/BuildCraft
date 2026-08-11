package buildcraft.api.v2.pipe;

@FunctionalInterface
public interface PipeConnectionRule {
    PipeConnectionDecision decide(PipeConnectionContext context);
}
