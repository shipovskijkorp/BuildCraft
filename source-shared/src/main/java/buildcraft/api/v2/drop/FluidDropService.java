package buildcraft.api.v2.drop;

import java.util.Collection;
import net.minecraft.world.item.ItemStack;

/** Resolves item drops for stored fluid without exposing loader FluidStack types. */
public interface FluidDropService {
    Collection<ItemStack> createDrops(FluidDropContext context);
}
