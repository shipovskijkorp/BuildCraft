package buildcraft.lib.internal.mj;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

public class MjBatteryTester {
    @Test
    void simulatedInsertionDoesNotMutateBattery() {
        MjBattery battery = new MjBattery(10_000);
        battery.addPower(4_000, FluidAction.SIMULATE);
        Assertions.assertEquals(0, battery.getStored());
        battery.addPower(4_000, FluidAction.EXECUTE);
        Assertions.assertEquals(4_000, battery.getStored());
    }

    @Test
    void extractionHonoursMinimumAndMaximum() {
        MjBattery battery = new MjBattery(10_000);
        battery.addPower(6_000, FluidAction.EXECUTE);
        Assertions.assertEquals(0, battery.extractPower(7_000, 8_000));
        Assertions.assertEquals(2_500, battery.extractPower(2_000, 2_500));
        Assertions.assertEquals(3_500, battery.getStored());
    }

    @Test
    void nbtRoundTripPreservesStoredPower() {
        MjBattery original = new MjBattery(10_000);
        original.addPower(7_500, FluidAction.EXECUTE);
        CompoundTag tag = original.serializeNBT();

        MjBattery restored = new MjBattery(10_000);
        restored.deserializeNBT(tag);
        Assertions.assertEquals(7_500, restored.getStored());
    }
}
