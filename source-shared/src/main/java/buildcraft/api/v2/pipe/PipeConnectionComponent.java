package buildcraft.api.v2.pipe;

public interface PipeConnectionComponent extends PipeComponent {
    PipeConnectionDecision connection(PipeConnectionContext context);
}
