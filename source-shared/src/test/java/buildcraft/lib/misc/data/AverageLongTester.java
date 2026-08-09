package buildcraft.lib.misc.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

public class AverageLongTester {
    @Test
    void nbtRoundTripPreservesAverage() {
        long value = 0x00DC_BA98_7654_3210L;
        AverageLong original = new AverageLong(5);
        for (int i = 0; i < 6; i++) {
            original.tick(value);
        }

        CompoundTag tag = new CompoundTag();
        original.writeToNbt(tag, "test");
        AverageLong restored = new AverageLong(5);
        restored.readFromNbt(tag, "test");
        Assertions.assertEquals(original.getAverageLong(), restored.getAverageLong());
    }

    @Test
    void pushValuesAreCombinedIntoOneTick() {
        AverageLong average = new AverageLong(4);
        average.push(10);
        average.push(6);
        average.tick();
        Assertions.assertEquals(4, average.getAverageLong());
        average.tick(8);
        Assertions.assertEquals(6, average.getAverageLong());
    }
}
