package buildcraft.test;

import org.junit.jupiter.api.Assertions;

import net.minecraft.world.phys.Vec3;

public final class TestHelper {
    private TestHelper() {
    }

    public static void assertVec3Equals(Vec3 expected, Vec3 actual) {
        Assertions.assertTrue(
            expected.distanceTo(actual) <= 1.0e-12,
            () -> actual + " was not equal to expected " + expected
        );
    }
}
