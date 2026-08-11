package buildcraft.api.v2.facade;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

public record FacadeStateResult(FacadeMaterial material, ItemStack sourceStack) {
    public FacadeStateResult {
        Objects.requireNonNull(material, "material");
        sourceStack = Objects.requireNonNull(sourceStack, "sourceStack").copy();
    }

    @Override public ItemStack sourceStack() { return sourceStack.copy(); }
    public Optional<ItemStack> source() { return sourceStack.isEmpty() ? Optional.empty() : Optional.of(sourceStack()); }
}
