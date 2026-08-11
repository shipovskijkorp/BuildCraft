package buildcraft.api.v2.network;

import buildcraft.api.v2.persistence.ApiCodec;
import buildcraft.api.v2.persistence.OpaqueData;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record PayloadType<T>(ResourceLocation id, PayloadDirection direction, PayloadPhase phase, int maxBytes, ApiCodec<T, OpaqueData> codec) {
    public PayloadType {
        Objects.requireNonNull(id, "id"); Objects.requireNonNull(direction, "direction"); Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(codec, "codec"); if (maxBytes <= 0) throw new IllegalArgumentException("maxBytes must be positive");
    }
}
