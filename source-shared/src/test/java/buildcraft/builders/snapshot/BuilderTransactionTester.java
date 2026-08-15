package buildcraft.builders.snapshot;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BuilderTransactionTester {
    @Test
    void legacyProgressConvertsBackToPhysicalBatteryReservation() {
        Assertions.assertEquals(0, SnapshotBuilder.legacyReservedPowerForProgress(0));
        Assertions.assertEquals(1, SnapshotBuilder.legacyReservedPowerForProgress(1));
        Assertions.assertEquals(1, SnapshotBuilder.legacyReservedPowerForProgress(2));
        Assertions.assertEquals(2, SnapshotBuilder.legacyReservedPowerForProgress(3));
        Assertions.assertEquals(50, SnapshotBuilder.legacyReservedPowerForProgress(100));
    }

    @Test
    void reservationAccountingSaturatesInsteadOfOverflowing() {
        Assertions.assertEquals(12, SnapshotBuilder.saturatingAdd(5, 7));
        Assertions.assertEquals(Long.MAX_VALUE, SnapshotBuilder.saturatingAdd(Long.MAX_VALUE - 2, 10));
        Assertions.assertEquals(5, SnapshotBuilder.saturatingAdd(5, -3));
    }
}
