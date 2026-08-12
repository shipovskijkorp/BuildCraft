package buildcraft.builders.internal.schematic.api2;

import buildcraft.api.v2.schematic.EntitySchematicAdapter;
import buildcraft.api.v2.schematic.SchematicAdapter;
import buildcraft.api.v2.schematic.SchematicCaptureContext;
import buildcraft.api.v2.schematic.SchematicEntityCaptureContext;
import buildcraft.api.v2.schematic.SchematicPlacementContext;
import buildcraft.api.v2.schematic.SchematicResult;
import buildcraft.api.v2.schematic.SnapshotElement;
import java.util.Optional;

/** Lossless no-op adapters used when an addon owning a persisted snapshot is absent. */
public final class UnavailableSchematicAdapters {
    public static final SchematicAdapter BLOCK = new SchematicAdapter() {
        @Override public int priority() { return Integer.MIN_VALUE; }
        @Override public boolean supports(SchematicCaptureContext context) { return false; }
        @Override public Optional<? extends SnapshotElement> capture(SchematicCaptureContext context) { return Optional.empty(); }
        @Override public boolean supportsElement(SnapshotElement element) { return true; }
        @Override public SchematicResult place(SnapshotElement element, SchematicPlacementContext context) {
            return new SchematicResult(SchematicResult.Status.FAILED, "Snapshot type is unavailable: " + element.typeId());
        }
    };

    public static final EntitySchematicAdapter ENTITY = new EntitySchematicAdapter() {
        @Override public int priority() { return Integer.MIN_VALUE; }
        @Override public boolean supports(SchematicEntityCaptureContext context) { return false; }
        @Override public Optional<? extends SnapshotElement> capture(SchematicEntityCaptureContext context) { return Optional.empty(); }
        @Override public boolean supportsElement(SnapshotElement element) { return true; }
        @Override public SchematicResult place(SnapshotElement element, SchematicPlacementContext context) {
            return new SchematicResult(SchematicResult.Status.FAILED, "Snapshot type is unavailable: " + element.typeId());
        }
    };

    private UnavailableSchematicAdapters() {}
}
