package buildcraft.lib.misc;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import net.minecraft.core.BlockPos;

public class PositionUtilTester {
    static Stream<Arguments> paths() {
        return Stream.of(
            Arguments.of(BlockPos.ZERO, BlockPos.ZERO),
            Arguments.of(new BlockPos(0, 1, 0), BlockPos.ZERO),
            Arguments.of(BlockPos.ZERO, new BlockPos(1, 4, 6)),
            Arguments.of(BlockPos.ZERO, new BlockPos(1, 0, 0)),
            Arguments.of(BlockPos.ZERO, new BlockPos(1, 1, 1)),
            Arguments.of(new BlockPos(-50, 45, 34), new BlockPos(-37, 7, -40))
        );
    }

    static Stream<Arguments> boxes() {
        return Stream.of(
            Arguments.of(BlockPos.ZERO, BlockPos.ZERO),
            Arguments.of(BlockPos.ZERO, new BlockPos(1, 0, 0)),
            Arguments.of(BlockPos.ZERO, new BlockPos(0, 1, 0)),
            Arguments.of(BlockPos.ZERO, new BlockPos(0, 0, 1)),
            Arguments.of(BlockPos.ZERO, new BlockPos(3, 3, 0)),
            Arguments.of(BlockPos.ZERO, new BlockPos(3, 0, 3)),
            Arguments.of(BlockPos.ZERO, new BlockPos(0, 3, 3)),
            Arguments.of(new BlockPos(-45, 3, -4), new BlockPos(-38, 16, 16))
        );
    }

    @ParameterizedTest
    @MethodSource("paths")
    void pathsExcludeStartIncludeEndAndMoveOnlyToNeighbours(BlockPos from, BlockPos to) {
        List<BlockPos> path = PositionUtil.getAllOnPath(from, to);

        if (from.equals(to)) {
            Assertions.assertTrue(path.isEmpty(), "equal endpoints must produce an empty path");
            return;
        }

        Assertions.assertFalse(path.isEmpty());
        Assertions.assertFalse(path.contains(from), "path must not contain its starting position");
        Assertions.assertEquals(to, path.get(path.size() - 1));
        Assertions.assertEquals(path.size(), new HashSet<>(path).size(), "path contains duplicate positions");

        BlockPos previous = from;
        for (BlockPos current : path) {
            int dx = Math.abs(current.getX() - previous.getX());
            int dy = Math.abs(current.getY() - previous.getY());
            int dz = Math.abs(current.getZ() - previous.getZ());
            Assertions.assertTrue(dx <= 1 && dy <= 1 && dz <= 1 && dx + dy + dz > 0,
                "non-neighbouring path step: " + previous + " -> " + current);
            previous = current;
        }
    }

    @ParameterizedTest
    @MethodSource("boxes")
    void edgeEnumerationIsUniqueCompleteAndConsistent(BlockPos min, BlockPos max) {
        List<BlockPos> edge = PositionUtil.getAllOnEdge(min, max);
        Assertions.assertEquals(edge.size(), new HashSet<>(edge).size(), "edge contains duplicate positions");
        Assertions.assertEquals(PositionUtil.getCountOnEdge(min, max), edge.size());

        for (BlockPos position : edge) {
            Assertions.assertTrue(PositionUtil.isOnEdge(min, max, position), "not on edge: " + position);
            Assertions.assertTrue(PositionUtil.isOnFace(min, max, position), "not on face: " + position);
        }

        for (BlockPos position : BlockPos.betweenClosed(min.offset(-1, -1, -1), max.offset(1, 1, 1))) {
            Assertions.assertEquals(
                PositionUtil.isOnEdge(min, max, position),
                edge.contains(position),
                "edge membership mismatch at " + position
            );
        }
    }
}
