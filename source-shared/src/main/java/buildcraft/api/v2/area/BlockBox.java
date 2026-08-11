package buildcraft.api.v2.area;

import java.util.Objects;
import net.minecraft.core.BlockPos;

/** Immutable inclusive block-space box. */
public record BlockBox(BlockPos min, BlockPos max) {
    public BlockBox {
        Objects.requireNonNull(min, "min");
        Objects.requireNonNull(max, "max");
        int minX = Math.min(min.getX(), max.getX());
        int minY = Math.min(min.getY(), max.getY());
        int minZ = Math.min(min.getZ(), max.getZ());
        int maxX = Math.max(min.getX(), max.getX());
        int maxY = Math.max(min.getY(), max.getY());
        int maxZ = Math.max(min.getZ(), max.getZ());
        min = new BlockPos(minX, minY, minZ);
        max = new BlockPos(maxX, maxY, maxZ);
    }

    public boolean contains(BlockPos pos) {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX()
            && pos.getY() >= min.getY() && pos.getY() <= max.getY()
            && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    public long volume() {
        return Math.multiplyExact(
            Math.multiplyExact((long) max.getX() - min.getX() + 1L, (long) max.getY() - min.getY() + 1L),
            (long) max.getZ() - min.getZ() + 1L
        );
    }
}
