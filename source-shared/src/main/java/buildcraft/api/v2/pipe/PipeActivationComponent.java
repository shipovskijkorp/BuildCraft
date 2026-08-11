package buildcraft.api.v2.pipe;

public interface PipeActivationComponent extends PipeComponent {
    PipeActivationResult activate(PipeActivationContext context);
}
