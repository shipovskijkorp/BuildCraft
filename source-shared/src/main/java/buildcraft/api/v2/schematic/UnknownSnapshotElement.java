package buildcraft.api.v2.schematic;

import buildcraft.api.v2.persistence.OpaqueData;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Snapshot element retained when its addon/type is temporarily unavailable. */
public record UnknownSnapshotElement(ResourceLocation typeId, int schemaVersion, OpaqueData payload) implements SnapshotElement {
    public UnknownSnapshotElement {
        Objects.requireNonNull(typeId, "typeId");
        if (schemaVersion < 0) throw new IllegalArgumentException("schemaVersion must be non-negative");
        Objects.requireNonNull(payload, "payload");
    }
}
