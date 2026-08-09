package buildcraft.lib.misc;

import java.util.stream.Stream;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import net.minecraft.network.FriendlyByteBuf;

public class MessageUtilTester {
    static Stream<Arguments> booleanArrays() {
        return Stream.of(
            Arguments.of((Object) new boolean[0]),
            Arguments.of((Object) new boolean[] { false, true, false }),
            Arguments.of((Object) new boolean[] {
                false, true, false, false, false, true, true, true, true, true, true, true, false
            })
        );
    }

    @ParameterizedTest
    @MethodSource("booleanArrays")
    void booleanArraysRoundTrip(boolean[] expected) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        MessageUtil.writeBooleanArray(buffer, expected);
        Assertions.assertArrayEquals(expected, MessageUtil.readBooleanArray(buffer, expected.length));
    }

    @ParameterizedTest
    @MethodSource("booleanArrays")
    void booleanArraysCanBeReadIntoExistingStorage(boolean[] expected) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        MessageUtil.writeBooleanArray(buffer, expected);
        boolean[] actual = new boolean[expected.length];
        MessageUtil.readBooleanArray(buffer, actual);
        Assertions.assertArrayEquals(expected, actual);
    }
}
