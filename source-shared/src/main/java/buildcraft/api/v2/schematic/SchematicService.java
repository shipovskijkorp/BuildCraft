package buildcraft.api.v2.schematic;

import java.util.Optional;

/** Runtime gateway used by BuildCraft and addons for blueprint/schematic elements. */
public interface SchematicService {
    Optional<? extends SnapshotElement> capture(SchematicCaptureContext context);

    default Optional<? extends SnapshotElement> captureEntity(SchematicEntityCaptureContext context) {
        return Optional.empty();
    }

    SchematicResult place(SnapshotElement element, SchematicPlacementContext context);
}
