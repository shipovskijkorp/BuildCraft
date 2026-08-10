package buildcraft.api.v2.recipe;

import buildcraft.api.v2.fluid.FluidVariantData;

/** Serializable distillation recipe form. Null output variants mean no output on that side. */
public record DistillationRecipeData(
    FluidIngredientData input,
    FluidVariantData gasVariant,
    long gasMilliBuckets,
    FluidVariantData liquidVariant,
    long liquidMilliBuckets,
    long powerRequiredMicroMj
) {}
