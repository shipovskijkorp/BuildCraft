package buildcraft.api.v2.persistence;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record KnownPayload<T, P>(
    ResourceLocation storedTypeId,
    ResourceLocation canonicalTypeId,
    int storedSchemaVersion,
    int currentSchemaVersion,
    T value
) implements PayloadResolution<T, P> {
    public KnownPayload {
        Objects.requireNonNull(storedTypeId, "storedTypeId");
        Objects.requireNonNull(canonicalTypeId, "canonicalTypeId");
        Objects.requireNonNull(value, "value");
        if (storedSchemaVersion < 0 || currentSchemaVersion < 0) {
            throw new IllegalArgumentException("Schema versions must be non-negative");
        }
    }

    @Override
    public boolean known() {
        return true;
    }
}
