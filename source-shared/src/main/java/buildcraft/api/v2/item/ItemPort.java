package buildcraft.api.v2.item;

import buildcraft.api.v2.OperationMode;
import net.minecraft.world.item.ItemStack;

/**
 * Loader-neutral item insertion/extraction endpoint.
 * Implementations MUST NOT mutate state during SIMULATE.
 */
public interface ItemPort {
    ItemTransferResult insert(ItemStack offered, OperationMode mode);

    ItemTransferResult extract(ItemMatcher matcher, int maxCount, OperationMode mode);
}
