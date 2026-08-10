package buildcraft.api.v2.recipe;

import buildcraft.api.v2.fluid.FluidVariantData;

/** Serializable Heat Exchanger recipe form. */
public record HeatExchangeRecipeData(
    RecipeDefinition.Kind kind,
    FluidIngredientData input,
    FluidVariantData outputVariant,
    long outputMilliBuckets,
    int heatFrom,
    int heatTo
) {}
