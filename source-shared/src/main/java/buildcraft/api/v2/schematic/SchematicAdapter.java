package buildcraft.api.v2.schematic;

import java.util.Optional;

public interface SchematicAdapter {
    int priority();
    boolean supports(SchematicCaptureContext context);
    Optional<? extends SnapshotElement> capture(SchematicCaptureContext context);
    SchematicResult place(SnapshotElement element, SchematicPlacementContext context);
}
