package buildcraft.api.v2.filler;

import buildcraft.api.v2.area.BlockBox;
import net.minecraft.core.BlockPos;

public interface FillerMask {
    BlockBox bounds();
    boolean includes(BlockPos pos);
}
