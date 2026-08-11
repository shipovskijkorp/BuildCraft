package buildcraft.api.v2.area;

import java.util.Optional;
import net.minecraft.core.BlockPos;

@FunctionalInterface
public interface Zone {
    boolean contains(BlockPos pos);

    default Optional<BlockBox> bounds() {
        return Optional.empty();
    }
}
