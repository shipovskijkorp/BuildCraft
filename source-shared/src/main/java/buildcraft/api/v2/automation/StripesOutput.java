package buildcraft.api.v2.automation;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

/** Output sink used by Stripes handlers to return or eject item stacks. */
public interface StripesOutput {
    boolean sendItem(ItemStack stack, Direction from);
    void dropItem(ItemStack stack, Direction from);

    static StripesOutput discard() {
        return new StripesOutput() {
            @Override public boolean sendItem(ItemStack stack, Direction from) { return false; }
            @Override public void dropItem(ItemStack stack, Direction from) {}
        };
    }
}
