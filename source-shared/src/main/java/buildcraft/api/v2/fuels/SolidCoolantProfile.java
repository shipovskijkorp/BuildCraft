package buildcraft.api.v2.fuels;

import buildcraft.api.v2.fluid.FluidVolume;
import buildcraft.api.v2.item.ItemMatcher;
import java.util.Objects;
import net.minecraft.world.item.ItemStack;

/** Immutable programmatic solid-coolant conversion. */
public final class SolidCoolantProfile implements EnergyFluidDefinition {
    private final ItemMatcher matcher;
    private final SolidCoolantConversion conversion;

    public SolidCoolantProfile(ItemMatcher matcher, SolidCoolantConversion conversion) {
        this.matcher = Objects.requireNonNull(matcher, "matcher");
        this.conversion = Objects.requireNonNull(conversion, "conversion");
    }

    @Override
    public Kind kind() { return Kind.SOLID_COOLANT; }
    public ItemMatcher matcher() { return matcher; }

    public boolean matches(ItemStack stack) {
        return stack != null && !stack.isEmpty() && matcher.matches(stack);
    }

    public FluidVolume convert(ItemStack stack) {
        if (!matches(stack)) return FluidVolume.empty();
        ItemStack copy = stack.copy();
        FluidVolume result = conversion.convert(copy);
        return result == null ? FluidVolume.empty() : result;
    }
}
