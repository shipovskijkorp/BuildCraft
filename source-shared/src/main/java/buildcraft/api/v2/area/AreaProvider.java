package buildcraft.api.v2.area;

import java.util.Optional;

public interface AreaProvider {
    default Optional<BlockBox> box() { return Optional.empty(); }
    default Optional<Zone> zone() { return Optional.empty(); }
    default Optional<Path> path() { return Optional.empty(); }
}
