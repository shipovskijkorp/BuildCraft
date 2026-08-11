package buildcraft.api.v2.area;

import java.util.List;
import net.minecraft.core.BlockPos;

public interface Path {
    List<BlockPos> points();
    default boolean empty() { return points().isEmpty(); }
}
