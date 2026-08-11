package buildcraft.api.v2.pipe;

@FunctionalInterface
public interface PipeEventListener<E> {
    void handle(PipeMutationContext pipe, E event);
}
