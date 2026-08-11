package buildcraft.api.v2.persistence;

import java.util.Arrays;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Immutable loader-neutral opaque payload for addon-owned runtime state. */
public final class OpaqueData {
    private final ResourceLocation format;
    private final byte[] bytes;

    public OpaqueData(ResourceLocation format, byte[] bytes) {
        this.format = Objects.requireNonNull(format, "format");
        this.bytes = Objects.requireNonNull(bytes, "bytes").clone();
    }

    public ResourceLocation format() { return format; }
    public byte[] bytes() { return bytes.clone(); }
    public int size() { return bytes.length; }

    @Override public boolean equals(Object other) {
        return other instanceof OpaqueData that && format.equals(that.format) && Arrays.equals(bytes, that.bytes);
    }

    @Override public int hashCode() {
        return 31 * format.hashCode() + Arrays.hashCode(bytes);
    }
}
