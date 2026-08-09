package buildcraft.lib.misc;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

public class HashUtilTester {
    @Test
    void hexadecimalConversionRoundTrips() {
        byte[] hash = { 0, 1, 5, 9, (byte) 0xff, (byte) 0xbc };
        String encoded = HashUtil.convertHashToString(hash);
        Assertions.assertEquals("00010509ffbc", encoded);
        Assertions.assertArrayEquals(hash, HashUtil.convertStringToHash(encoded));
    }

    @Test
    void compoundHashDoesNotDependOnInsertionOrder() {
        CompoundTag first = new CompoundTag();
        first.putString("text", "value");
        first.putInt("number", 42);

        CompoundTag second = new CompoundTag();
        second.putInt("number", 42);
        second.putString("text", "value");

        Assertions.assertArrayEquals(HashUtil.computeHash(first), HashUtil.computeHash(second));
        second.putInt("number", 43);
        Assertions.assertFalse(Arrays.equals(HashUtil.computeHash(first), HashUtil.computeHash(second)));
    }

    @Test
    void byteHashUsesSha256() {
        Assertions.assertEquals(64, HashUtil.convertHashToString(HashUtil.computeHash(new byte[0])).length());
        Assertions.assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            HashUtil.convertHashToString(HashUtil.computeHash(new byte[0]))
        );
    }
}
