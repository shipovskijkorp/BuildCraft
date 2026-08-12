package buildcraft.api.v2.schematic;

import buildcraft.api.v2.persistence.OpaqueData;
import buildcraft.api.v2.persistence.PersistentType;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record SnapshotElementType<E extends SnapshotElement>(ResourceLocation id, PersistentType<E, OpaqueData> persistence) {
    public SnapshotElementType {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(persistence, "persistence");
        if (!id.equals(persistence.id())) {
            throw new IllegalArgumentException("Snapshot element id must match persistence id: " + id + " != " + persistence.id());
        }
    }
}
