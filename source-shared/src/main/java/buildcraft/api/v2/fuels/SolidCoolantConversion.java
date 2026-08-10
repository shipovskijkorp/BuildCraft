package buildcraft.api.v2.fuels;

import buildcraft.api.v2.fluid.FluidVolume;
import net.minecraft.world.item.ItemStack;

/** Converts a candidate solid coolant stack into a fluid volume without mutating the input stack. */
@FunctionalInterface
public interface SolidCoolantConversion {
    FluidVolume convert(ItemStack stack);
}
