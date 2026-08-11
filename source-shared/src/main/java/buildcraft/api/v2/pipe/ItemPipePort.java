package buildcraft.api.v2.pipe;

import buildcraft.api.v2.OperationMode;
import buildcraft.api.v2.item.ItemPort;
import buildcraft.api.v2.item.ItemTransferResult;

/** Item port that can preserve pipe-specific transit metadata such as color and speed. */
public interface ItemPipePort extends ItemPort {
    ItemTransferResult inject(ItemInjectionRequest request, OperationMode mode);
}
