package buildcraft.api.v2.pipe;

public interface PipeTickComponent extends PipeComponent {
    void tick(PipeMutationContext context);
}
