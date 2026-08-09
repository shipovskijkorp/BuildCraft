package buildcraft.lib.nbt;

import java.io.IOException;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import buildcraft.api.data.NbtSquishConstants;

public class NbtSquisherTester {
    static IntStream formats() {
        return IntStream.of(
            NbtSquishConstants.VANILLA,
            NbtSquishConstants.VANILLA_COMPRESSED,
            NbtSquishConstants.BUILDCRAFT_V1,
            NbtSquishConstants.BUILDCRAFT_V1_COMPRESSED
        );
    }

    @ParameterizedTest
    @MethodSource("formats")
    void everySupportedFormatRoundTripsComplexNbt(int format) throws IOException {
        CompoundTag original = createTag();
        byte[] encoded = NbtSquisher.squish(original, format);
        CompoundTag restored = NbtSquisher.expand(encoded);

        Assertions.assertArrayEquals(original.getAllKeys().stream().sorted().toArray(),
            restored.getAllKeys().stream().sorted().toArray());
        Assertions.assertEquals(original, restored);
    }

    private static CompoundTag createTag() {
        CompoundTag root = new CompoundTag();
        root.putByte("byte", (byte) 1);
        root.putShort("short", (short) 2);
        root.putInt("int", 3);
        root.putLong("long", 4L);
        root.putFloat("float", 5.5F);
        root.putDouble("double", 6.25D);
        root.putByteArray("bytes", new byte[] { 1, 2, 3, 4 });
        root.putIntArray("ints", new int[] { 10, 20, 30 });
        root.putString("string", "BuildCraft");

        ListTag list = new ListTag();
        list.add(StringTag.valueOf("first"));
        list.add(StringTag.valueOf("second"));
        root.put("list", list);

        CompoundTag nested = new CompoundTag();
        nested.putBoolean("enabled", true);
        nested.putString("id", "buildcraft:test");
        root.put("nested", nested);
        return root;
    }
}
