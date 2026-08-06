package buildcraft.api.transport.pipe;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PipeFaceTexTester {
    @Test
    void cachedAndFreshSingleTexturesAreEqual() {
        for (int i = 0; i < 1000; i++) {
            assertEquivalent(PipeFaceTex.___testing_create_single(i), PipeFaceTex.get(i));
        }
        assertEquivalent(PipeFaceTex.get(0), PipeFaceTex.get(new int[] { 0 }, -1));
    }

    @Test
    void alphaBitsDoNotAffectColourIdentity() {
        assertEquivalent(
            PipeFaceTex.get(new int[] { 7 }, 0x12_33_66_99),
            PipeFaceTex.get(new int[] { 7 }, 0xFF_33_66_99)
        );
    }

    private static void assertEquivalent(PipeFaceTex first, PipeFaceTex second) {
        Assertions.assertEquals(first, second);
        Assertions.assertEquals(first.hashCode(), second.hashCode());
    }
}
