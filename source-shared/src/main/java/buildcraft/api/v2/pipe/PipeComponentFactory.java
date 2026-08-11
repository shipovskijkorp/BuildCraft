package buildcraft.api.v2.pipe;

@FunctionalInterface
public interface PipeComponentFactory<C extends PipeComponent> {
    C create(PipeView pipe);
}
