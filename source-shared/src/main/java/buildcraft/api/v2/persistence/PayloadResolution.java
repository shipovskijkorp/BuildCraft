package buildcraft.api.v2.persistence;

import net.minecraft.resources.ResourceLocation;

/**
 * Result of resolving and decoding one persisted extension envelope.
 */
public sealed interface PayloadResolution<T, P> permits KnownPayload, UnknownPayload {
    ResourceLocation storedTypeId();

    int storedSchemaVersion();

    boolean known();
}
