package buildcraft.api.v2.fluid;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * Opaque, immutable component/NBT payload belonging to a fluid variant.
 *
 * Common addon code can preserve and compare the payload without depending on
 * Forge, NeoForge or Fabric stack/component types. The format id is owned by
 * the platform bridge or addon that produced the canonical bytes.
 */
public final class FluidComponentPayload {
    private static final FluidComponentPayload EMPTY = new FluidComponentPayload(null, new byte[0]);

    private final ResourceLocation formatId;
    private final byte[] canonicalBytes;

    private FluidComponentPayload(ResourceLocation formatId, byte[] canonicalBytes) {
        this.formatId = formatId;
        this.canonicalBytes = canonicalBytes;
    }

    public static FluidComponentPayload empty() {
        return EMPTY;
    }

    public static FluidComponentPayload of(ResourceLocation formatId, byte[] canonicalBytes) {
        Objects.requireNonNull(formatId, "formatId");
        Objects.requireNonNull(canonicalBytes, "canonicalBytes");
        if (canonicalBytes.length == 0) {
            throw new IllegalArgumentException("Non-empty fluid component payload must contain canonical data");
        }
        return new FluidComponentPayload(formatId, canonicalBytes.clone());
    }

    public boolean isEmpty() {
        return formatId == null;
    }

    public Optional<ResourceLocation> formatId() {
        return Optional.ofNullable(formatId);
    }

    public byte[] copyCanonicalBytes() {
        return canonicalBytes.clone();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof FluidComponentPayload other)) {
            return false;
        }
        return Objects.equals(formatId, other.formatId) && Arrays.equals(canonicalBytes, other.canonicalBytes);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hashCode(formatId) + Arrays.hashCode(canonicalBytes);
    }

    @Override
    public String toString() {
        return isEmpty() ? "FluidComponentPayload[empty]" : "FluidComponentPayload[format=" + formatId + ", bytes=" + canonicalBytes.length + "]";
    }
}
