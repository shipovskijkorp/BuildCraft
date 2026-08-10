package buildcraft.api.v2.persistence;

/**
 * Serialization contract independent from Forge, NeoForge and Fabric.
 *
 * P is the serialized representation used by the domain. It may be NBT,
 * JSON, or another representation owned by the caller/platform bridge.
 */
public interface ApiCodec<T, P> {
    CodecResult<T> decode(P payload);

    CodecResult<P> encode(T value);
}
