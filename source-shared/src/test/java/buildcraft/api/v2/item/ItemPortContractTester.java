package buildcraft.api.v2.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/** Pure JVM contract checks. ItemStack copy semantics are covered by launched GameTests. */
public class ItemPortContractTester {
    @Test
    public void invalidCountsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> ItemTransferResult.nothing(-1));
    }
}
