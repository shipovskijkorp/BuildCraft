package buildcraft.api.v2.item;

import buildcraft.api.v2.OperationMode;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

/** Resolves registered label adapters without requiring item implementation inheritance. */
public interface ItemLabelService {
    Optional<ItemLabelAdapter> adapter(ItemStack stack);

    default Optional<String> label(ItemStack stack) {
        return adapter(stack).map(a -> a.requireLabel(stack));
    }

    default boolean setLabel(ItemStack stack, String label, OperationMode mode) {
        return adapter(stack).map(a -> a.setLabel(stack, label, mode)).orElse(false);
    }
}
