package buildcraft.builders.snapshot;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import net.minecraft.core.BlockPos;

public class PosIndexTester {
    private static final BlockPos SIZE = new BlockPos(6, 4, 8);

    static Stream<BlockPos> positions() {
        return IntStream.range(0, Snapshot.getDataSize(SIZE)).mapToObj(i -> Snapshot.indexToPos(SIZE, i));
    }

    @ParameterizedTest
    @MethodSource("positions")
    void positionIndexRoundTrip(BlockPos position) {
        int index = Snapshot.posToIndex(SIZE, position);
        Assertions.assertEquals(position, Snapshot.indexToPos(SIZE, index), "index=" + index);
    }

    @ParameterizedTest
    @MethodSource("positions")
    void indexPositionRoundTrip(BlockPos position) {
        int index = Snapshot.posToIndex(SIZE, position);
        Assertions.assertEquals(index, Snapshot.posToIndex(SIZE, Snapshot.indexToPos(SIZE, index)));
    }
}
