package buildcraft.api.v2.fluid;

import buildcraft.api.v2.persistence.ApiCodec;
import buildcraft.api.v2.persistence.CodecResult;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * Immutable identity of one fluid variant.
 *
 * Equality includes opaque component data. Use {@link #sameFluid(FluidVariant)}
 * when only the registry id should be compared.
 */
public final class FluidVariant {
    public static final ApiCodec<FluidVariant, FluidVariantData> CODEC = new ApiCodec<>() {
        @Override
        public CodecResult<FluidVariant> decode(FluidVariantData payload) {
            if (payload == null) {
                return CodecResult.failure("Fluid variant payload is null");
            }
            return CodecResult.success(FluidVariant.of(payload.fluidId(), payload.components()));
        }

        @Override
        public CodecResult<FluidVariantData> encode(FluidVariant value) {
            if (value == null) {
                return CodecResult.failure("Fluid variant is null");
            }
            return CodecResult.success(new FluidVariantData(value.fluidId, value.components));
        }
    };

    private final ResourceLocation fluidId;
    private final FluidComponentPayload components;

    private FluidVariant(ResourceLocation fluidId, FluidComponentPayload components) {
        this.fluidId = fluidId;
        this.components = components;
    }

    public static FluidVariant of(ResourceLocation fluidId) {
        return of(fluidId, FluidComponentPayload.empty());
    }

    public static FluidVariant of(ResourceLocation fluidId, FluidComponentPayload components) {
        return new FluidVariant(Objects.requireNonNull(fluidId, "fluidId"), Objects.requireNonNull(components, "components"));
    }

    public ResourceLocation fluidId() {
        return fluidId;
    }

    public FluidComponentPayload components() {
        return components;
    }

    public boolean sameFluid(FluidVariant other) {
        return other != null && fluidId.equals(other.fluidId);
    }

    public boolean sameVariant(FluidVariant other) {
        return equals(other);
    }

    /**
     * Stable FNV-1a hash derived from registry id, component format and canonical
     * payload bytes. This does not depend on JVM identity hash codes.
     */
    public long stableHash64() {
        long hash = 0xcbf29ce484222325L;
        hash = fnv(hash, fluidId.toString().getBytes(StandardCharsets.UTF_8));
        hash = fnv(hash, new byte[] {0});
        if (!components.isEmpty()) {
            hash = fnv(hash, components.formatId().orElseThrow().toString().getBytes(StandardCharsets.UTF_8));
            hash = fnv(hash, new byte[] {0});
            hash = fnv(hash, components.copyCanonicalBytes());
        }
        return hash;
    }

    private static long fnv(long hash, byte[] bytes) {
        for (byte value : bytes) {
            hash ^= value & 0xffL;
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this
            || obj instanceof FluidVariant other
            && fluidId.equals(other.fluidId)
            && components.equals(other.components);
    }

    @Override
    public int hashCode() {
        return 31 * fluidId.hashCode() + components.hashCode();
    }

    @Override
    public String toString() {
        return components.isEmpty() ? fluidId.toString() : fluidId + " " + components;
    }
}
