package buildcraft.api.v2.drop;

import java.util.Collection;
import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface FluidDropProvider {
    Collection<ItemStack> createDrops(FluidDropContext context);
}
