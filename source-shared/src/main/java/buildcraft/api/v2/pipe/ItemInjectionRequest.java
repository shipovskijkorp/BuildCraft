package buildcraft.api.v2.pipe;

import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public record ItemInjectionRequest(ItemStack stack, Direction from, ItemTransitData transit) {
    public ItemInjectionRequest {
        stack = Objects.requireNonNull(stack, "stack").copy();
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(transit, "transit");
    }

    @Override public ItemStack stack() { return stack.copy(); }
}
