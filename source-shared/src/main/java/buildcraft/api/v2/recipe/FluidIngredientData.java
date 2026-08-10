package buildcraft.api.v2.recipe;

import buildcraft.api.v2.fluid.FluidVariantData;

/** Serializable exact-fluid ingredient form. */
public record FluidIngredientData(FluidVariantData variant, long milliBuckets) {}
