package buildcraft.api.v2.request;

import buildcraft.api.v2.OperationMode;
import buildcraft.api.v2.item.ItemTransferResult;
import java.util.Collection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Slot-independent request provider replacing the index-based legacy IRequestProvider. */
public interface RequestProvider {
    Collection<ItemRequest> requests();
    ItemTransferResult offer(ResourceLocation requestId, ItemStack offered, OperationMode mode);
}
