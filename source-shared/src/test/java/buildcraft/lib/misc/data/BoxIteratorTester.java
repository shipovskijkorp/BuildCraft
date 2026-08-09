package buildcraft.lib.misc.data;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;

import buildcraft.lib.misc.data.AxisOrder.Inversion;

public class BoxIteratorTester {
    @Test
    void moveToMakesTheRequestedPositionNext() {
        for (int sx = 1; sx < 6; sx++) {
            for (int sy = 1; sy < 6; sy++) {
                for (int sz = 1; sz < 6; sz++) {
                    BoxIterator iterator = new BoxIterator(
                        BlockPos.ZERO,
                        new BlockPos(sx - 1, sy - 1, sz - 1),
                        AxisOrder.getFor(EnumAxisOrder.XYZ, Inversion.PPP),
                        false
                    );
                    Random random = new Random(42);
                    for (int i = 0; i < 200; i++) {
                        BlockPos requested = new BlockPos(random.nextInt(sx), random.nextInt(sy), random.nextInt(sz));
                        iterator.moveTo(requested);
                        Assertions.assertEquals(requested, iterator.advance());
                    }
                }
            }
        }
    }

    @Test
    void completeTraversalVisitsEveryPositionExactlyOnce() {
        BlockPos min = new BlockPos(-2, 3, 1);
        BlockPos max = new BlockPos(2, 5, 4);
        BoxIterator iterator = new BoxIterator(min, max, AxisOrder.getFor(EnumAxisOrder.ZYX, Inversion.NPN), false);
        Set<BlockPos> visited = new HashSet<>();
        while (iterator.hasNext()) {
            Assertions.assertTrue(visited.add(iterator.next()), "iterator visited a position twice");
        }
        int expected = (max.getX() - min.getX() + 1)
            * (max.getY() - min.getY() + 1)
            * (max.getZ() - min.getZ() + 1);
        Assertions.assertEquals(expected, visited.size());
    }
}
