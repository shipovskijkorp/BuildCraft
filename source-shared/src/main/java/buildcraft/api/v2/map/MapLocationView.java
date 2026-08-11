package buildcraft.api.v2.map;

import buildcraft.api.v2.area.BlockBox;
import buildcraft.api.v2.area.Path;
import buildcraft.api.v2.area.Zone;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Immutable semantic view of an encoded map-location item. */
public record MapLocationView(
    MapLocationKind kind,
    String label,
    Optional<BlockPos> point,
    Optional<Direction> pointSide,
    Optional<BlockBox> box,
    Optional<Zone> zone,
    Optional<Path> path
) {
    public MapLocationView {
        Objects.requireNonNull(kind, "kind");
        label = Objects.requireNonNull(label, "label");
        point = Objects.requireNonNull(point, "point");
        pointSide = Objects.requireNonNull(pointSide, "pointSide");
        box = Objects.requireNonNull(box, "box");
        zone = Objects.requireNonNull(zone, "zone");
        path = Objects.requireNonNull(path, "path");
        if (kind == MapLocationKind.CLEAN && (point.isPresent() || box.isPresent() || zone.isPresent() || path.isPresent())) {
            throw new IllegalArgumentException("CLEAN map location must not carry geometry");
        }
    }

    public static MapLocationView clean(String label) {
        return new MapLocationView(MapLocationKind.CLEAN, label, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
}
