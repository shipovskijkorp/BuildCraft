package buildcraft.api.v2.network;

import java.util.UUID;

public interface NetworkService {
    <T> void registerHandler(PayloadType<T> type, PayloadHandler<T> handler);
    <T> void sendToServer(PayloadType<T> type, T payload);
    <T> void sendToPlayer(UUID playerId, PayloadType<T> type, T payload);
}
