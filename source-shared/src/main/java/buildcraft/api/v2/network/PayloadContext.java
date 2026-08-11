package buildcraft.api.v2.network;

import java.util.Optional;
import java.util.UUID;

public record PayloadContext(PayloadDirection direction, PayloadPhase phase, UUID playerId) {
    public Optional<UUID> player() { return Optional.ofNullable(playerId); }
}
