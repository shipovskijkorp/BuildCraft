package buildcraft.core;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import buildcraft.lib.misc.data.Box;
import buildcraft.test.TestHelper;

public class BoxTester {
    private static final BlockPos MIN = new BlockPos(1, 2, 3);
    private static final BlockPos MAX = new BlockPos(4, 5, 6);

    static Stream<Arguments> containsPoints() {
        return Stream.of(
            Arguments.of(new Vec3(0, 0, 0), false),
            Arguments.of(new Vec3(1, 2, 3), true),
            Arguments.of(new Vec3(1.3, 2.4, 3.5), true),
            Arguments.of(new Vec3(4.9, 5.9, 6.9), true),
            Arguments.of(new Vec3(5, 5, 6), false)
        );
    }

    @ParameterizedTest
    @MethodSource("containsPoints")
    void containsUsesInclusiveBlockBoundsAndExclusiveOuterBounds(Vec3 point, boolean expected) {
        Assertions.assertEquals(expected, new Box(MIN, MAX).contains(point));
    }

    @Test
    void exposesNormalizedBoundsSizeAndCenter() {
        Box box = new Box(MAX, MIN);
        Assertions.assertEquals(MIN, box.min());
        Assertions.assertEquals(MAX, box.max());
        Assertions.assertEquals(new BlockPos(4, 4, 4), box.size());
        Assertions.assertEquals(new BlockPos(3, 4, 5), box.center());
        TestHelper.assertVec3Equals(new Vec3(3, 4, 5), box.centerExact());
    }

    @Test
    void intersectionIsSymmetricAndIncludesTouchingBlocks() {
        Box first = new Box(new BlockPos(0, 0, 0), new BlockPos(2, 2, 2));
        Box second = new Box(new BlockPos(1, 1, 1), new BlockPos(3, 3, 3));
        Box expected = new Box(new BlockPos(1, 1, 1), new BlockPos(2, 2, 2));
        Assertions.assertEquals(expected, first.getIntersect(second));
        Assertions.assertEquals(expected, second.getIntersect(first));

        Box touching = new Box(new BlockPos(2, 2, 2), new BlockPos(4, 4, 4));
        Assertions.assertEquals(
            new Box(new BlockPos(2, 2, 2), new BlockPos(2, 2, 2)),
            first.getIntersect(touching)
        );
    }

    @Test
    void disjointBoxesHaveNoIntersection() {
        Box first = new Box(BlockPos.ZERO, new BlockPos(1, 1, 1));
        Box second = new Box(new BlockPos(2, 2, 2), new BlockPos(3, 3, 3));
        Assertions.assertNull(first.getIntersect(second));
    }
}
