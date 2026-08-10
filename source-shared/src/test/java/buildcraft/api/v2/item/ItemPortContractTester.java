package buildcraft.api.v2.item;

import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ItemPortContractTester {
    @Test
    public void insertionResultUsesDefensiveCopy() {
        ItemStack offered = ItemStack.EMPTY;
        ItemTransferResult result = ItemTransferResult.ofInsertion(offered, 0);
        assertEquals(0, result.transferredCount());
        assertEquals(0, result.remainderCount());
    }

    @Test
    public void invalidCountsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> ItemTransferResult.nothing(-1));
    }
}
