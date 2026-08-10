package buildcraft.api.v2.fuels;

import buildcraft.api.v2.fluid.FluidVariantData;
import net.minecraft.resources.ResourceLocation;

/** Structured serializable form of a built-in fluid selector. */
public record FluidSelectorData(Kind kind, ResourceLocation id, FluidVariantData variant) {
    public enum Kind {
        FLUID,
        TAG,
        EXACT_VARIANT
    }
}
