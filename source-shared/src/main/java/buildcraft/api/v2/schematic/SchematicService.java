package buildcraft.api.v2.schematic;

import java.util.Optional;

public interface SchematicService {
    Optional<? extends SnapshotElement> capture(SchematicCaptureContext context);
    SchematicResult place(SnapshotElement element, SchematicPlacementContext context);
}
