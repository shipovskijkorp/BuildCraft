package buildcraft.api.v2.schematic;

import java.util.Optional;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;

/**
 * Addon extension point for entity snapshot capture and placement.
 * Implementations must not depend on BuildCraft implementation classes.
 */
public interface EntitySchematicAdapter {
    int priority();
    boolean supports(SchematicEntityCaptureContext context);
    Optional<? extends SnapshotElement> capture(SchematicEntityCaptureContext context);

    /** Return true when this adapter owns an already captured element. */
    default boolean supportsElement(SnapshotElement element) { return false; }

    default Vec3 relativePosition(SnapshotElement element) { return Vec3.ZERO; }

    default SchematicRequirements requirements(SnapshotElement element, SchematicPlacementContext context) {
        return SchematicRequirements.empty();
    }

    default SnapshotElement rotate(SnapshotElement element, Rotation rotation) { return element; }

    SchematicResult place(SnapshotElement element, SchematicPlacementContext context);
}
