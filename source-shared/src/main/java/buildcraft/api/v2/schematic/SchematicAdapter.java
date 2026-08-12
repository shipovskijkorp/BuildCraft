package buildcraft.api.v2.schematic;

import buildcraft.api.v2.OperationMode;
import buildcraft.api.v2.fluid.FluidVolume;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

/**
 * Addon extension point for block snapshot capture and placement.
 *
 * The small required core is capture + place. Optional hooks let BuildCraft's
 * Builder preserve the same scheduling/resource semantics for addon-owned
 * elements without exposing Builder implementation classes.
 */
public interface SchematicAdapter {
    int priority();
    boolean supports(SchematicCaptureContext context);
    Optional<? extends SnapshotElement> capture(SchematicCaptureContext context);

    /** Return true when this adapter owns an already captured element. */
    default boolean supportsElement(SnapshotElement element) { return false; }

    default boolean isAir(SnapshotElement element) { return false; }
    default Set<BlockPos> requiredBlockOffsets(SnapshotElement element) { return Set.of(); }

    default SchematicRequirements requirements(SnapshotElement element, Level level) {
        return SchematicRequirements.empty();
    }

    default SchematicRequirements placementRequirements(SnapshotElement element, Level level) {
        return requirements(element, level);
    }

    default List<ItemStack> deferredItems(SnapshotElement element, Level level) { return List.of(); }
    default List<ItemStack> missingDeferredItems(SnapshotElement element, Level level, BlockPos position) {
        return deferredItems(element, level);
    }

    /** Returns the remainder that was not accepted by an already placed block. */
    default ItemStack insertDeferredItem(
        SnapshotElement element,
        Level level,
        BlockPos position,
        ItemStack stack,
        OperationMode mode
    ) {
        return stack.copy();
    }

    default List<FluidVolume> requiredFluids(SnapshotElement element, Level level) {
        return requirements(element, level).fluids();
    }

    default SnapshotElement rotate(SnapshotElement element, Rotation rotation) { return element; }
    default boolean canPlace(SnapshotElement element, SchematicPlacementContext context) { return true; }
    default boolean readyToPlace(SnapshotElement element, SchematicPlacementContext context) { return true; }
    default boolean isPlaced(SnapshotElement element, Level level, BlockPos position) { return false; }

    SchematicResult place(SnapshotElement element, SchematicPlacementContext context);

    default SchematicResult placeUnchecked(SnapshotElement element, SchematicPlacementContext context) {
        return place(element, context);
    }
}
