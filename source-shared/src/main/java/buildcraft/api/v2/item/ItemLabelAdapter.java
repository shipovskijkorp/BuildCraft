package buildcraft.api.v2.item;

import buildcraft.api.v2.OperationMode;
import java.util.Objects;
import net.minecraft.world.item.ItemStack;

/** Adapter for items whose user-visible label can be read or changed. */
public interface ItemLabelAdapter {
    boolean supports(ItemStack stack);
    String label(ItemStack stack);
    boolean setLabel(ItemStack stack, String label, OperationMode mode);

    default String requireLabel(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        if (!supports(stack)) throw new IllegalArgumentException("Unsupported item stack");
        return Objects.requireNonNull(label(stack), "label(stack)");
    }
}
