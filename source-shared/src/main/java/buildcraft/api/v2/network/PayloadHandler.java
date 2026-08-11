package buildcraft.api.v2.network;

@FunctionalInterface
public interface PayloadHandler<T> {
    void handle(T payload, PayloadContext context);
}
