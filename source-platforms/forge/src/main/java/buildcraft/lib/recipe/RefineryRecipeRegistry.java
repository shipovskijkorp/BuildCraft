/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 */
package buildcraft.lib.recipe;

import buildcraft.api.recipes.IRefineryRecipeManager;
import buildcraft.api.v2.fluid.FluidVolume;
import buildcraft.api.v2.recipe.DistillationRecipeDefinition;
import buildcraft.api.v2.recipe.FluidIngredient;
import buildcraft.api.v2.recipe.HeatExchangeRecipeDefinition;
import buildcraft.api.v2.recipe.RecipeDefinition;
import buildcraft.api.v2.recipe.RecipeMatch;
import buildcraft.api.v2.reload.DefinitionProvenance;
import buildcraft.lib.internal.api.v2.BuildCraftApiRuntime;
import buildcraft.lib.fluid.FluidCompatRegistry;
import buildcraft.lib.fluid.FuelApiBridge;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

/** Legacy refinery API backed by the API 2 machine-recipe snapshot. */
public enum RefineryRecipeRegistry implements IRefineryRecipeManager {
    INSTANCE;

    private static final DefinitionProvenance LEGACY_PROVENANCE =
        new DefinitionProvenance("legacy-api", "refinery-recipe", 0);

    private final Map<ResourceLocation, IRefineryRecipe> legacyObjects = new HashMap<>();
    private final IRefineryRegistry<IDistillationRecipe> distillationRegistry = new SingleRegistry<>(RecipeDefinition.Kind.DISTILLATION);
    private final IRefineryRegistry<IHeatableRecipe> heatableRegistry = new SingleRegistry<>(RecipeDefinition.Kind.HEATING);
    private final IRefineryRegistry<ICoolableRecipe> coolableRegistry = new SingleRegistry<>(RecipeDefinition.Kind.COOLING);

    @Override
    public IHeatableRecipe createHeatingRecipe(FluidStack in, FluidStack out, int heatFrom, int heatTo) {
        return new HeatableRecipe(in, out, heatFrom, heatTo);
    }

    @Override
    public ICoolableRecipe createCoolableRecipe(FluidStack in, FluidStack out, int heatFrom, int heatTo) {
        return new CoolableRecipe(in, out, heatFrom, heatTo);
    }

    @Override
    public IDistillationRecipe createDistillationRecipe(FluidStack in, FluidStack outGas, FluidStack outLiquid, long powerRequired) {
        return new DistillationRecipe(powerRequired, in, outGas, outLiquid);
    }

    @Override public IRefineryRegistry<IHeatableRecipe> getHeatableRegistry() { return heatableRegistry; }
    @Override public IRefineryRegistry<ICoolableRecipe> getCoolableRegistry() { return coolableRegistry; }
    @Override public IRefineryRegistry<IDistillationRecipe> getDistillationRegistry() { return distillationRegistry; }

    private final class SingleRegistry<R extends IRefineryRecipe> implements IRefineryRegistry<R> {
        private final RecipeDefinition.Kind kind;

        private SingleRegistry(RecipeDefinition.Kind kind) { this.kind = kind; }

        @Override
        public Stream<R> getRecipes(Predicate<R> filter) {
            Objects.requireNonNull(filter, "filter");
            return getAllRecipes().stream().filter(filter);
        }

        @Override
        public Collection<R> getAllRecipes() {
            List<R> result = new ArrayList<>();
            for (RecipeMatch<? extends RecipeDefinition> match : matches(kind)) {
                R value = legacyView(match);
                if (value != null) result.add(value);
            }
            return List.copyOf(result);
        }

        @Override
        @Nullable
        public R getRecipeForInput(@Nullable FluidStack fluid) {
            if (fluid == null || fluid.isEmpty()) return null;
            FluidStack canonical = FluidCompatRegistry.canonicalize(fluid);
            for (RecipeMatch<? extends RecipeDefinition> match : matches(kind)) {
                IRefineryRecipe legacy = legacyObjects.get(match.id());
                if (legacy != null) {
                    if (FluidCompatRegistry.areEquivalent(legacy.in(), fluid)) return castLegacy(legacy);
                    continue;
                }
                if (inputOf(match.recipe()).matches(FuelApiBridge.variantOf(canonical), FuelApiBridge.MATCH_CONTEXT)) {
                    return legacyView(match);
                }
            }
            return null;
        }

        @Override
        public Collection<R> removeRecipes(Predicate<R> toRemove) {
            Objects.requireNonNull(toRemove, "toRemove");
            List<R> removed = new ArrayList<>();
            for (RecipeMatch<? extends RecipeDefinition> match : new ArrayList<>(matches(kind))) {
                R view = legacyView(match);
                if (view != null && toRemove.test(view) && BuildCraftApiRuntime.INSTANCE.machineRecipes().removeCode(match.id())) {
                    legacyObjects.remove(match.id());
                    removed.add(view);
                }
            }
            return List.copyOf(removed);
        }

        @Override
        public R addRecipe(R recipe) {
            Objects.requireNonNull(recipe, "recipe");
            if (recipe.in() == null || recipe.in().isEmpty() || recipe.in().getAmount() <= 0) {
                throw new IllegalArgumentException("Refinery recipe input must be non-empty with a positive amount");
            }
            ResourceLocation id = syntheticId(kind, recipe.in());
            RecipeDefinition definition = toV2(kind, recipe);
            BuildCraftApiRuntime.INSTANCE.machineRecipes().replaceCode(
                id, definition,
                new DefinitionProvenance(LEGACY_PROVENANCE.owner(), "refinery:" + id, LEGACY_PROVENANCE.priority())
            );
            legacyObjects.put(id, recipe);
            return recipe;
        }

        @SuppressWarnings("unchecked")
        private R castLegacy(IRefineryRecipe recipe) { return (R) recipe; }

        @SuppressWarnings("unchecked")
        private R legacyView(RecipeMatch<? extends RecipeDefinition> match) {
            IRefineryRecipe existing = legacyObjects.get(match.id());
            if (existing != null) return (R) existing;
            RecipeDefinition definition = match.recipe();
            if (definition instanceof DistillationRecipeDefinition distillation) {
                return kind == RecipeDefinition.Kind.DISTILLATION ? (R) new DistillationRecipe(
                    distillation.powerRequiredMicroMj(), inputStack(distillation.input()),
                    FuelApiBridge.stackOf(distillation.gasOutput()), FuelApiBridge.stackOf(distillation.liquidOutput())
                ) : null;
            }
            if (definition instanceof HeatExchangeRecipeDefinition heat) {
                FluidStack out = FuelApiBridge.stackOf(heat.output());
                if (kind == RecipeDefinition.Kind.HEATING && heat.kind() == RecipeDefinition.Kind.HEATING) {
                    return (R) new HeatableRecipe(inputStack(heat.input()), out.isEmpty() ? null : out, heat.heatFrom(), heat.heatTo());
                }
                if (kind == RecipeDefinition.Kind.COOLING && heat.kind() == RecipeDefinition.Kind.COOLING) {
                    return (R) new CoolableRecipe(inputStack(heat.input()), out.isEmpty() ? null : out, heat.heatFrom(), heat.heatTo());
                }
            }
            return null;
        }
    }

    private List<RecipeMatch<? extends RecipeDefinition>> matches(RecipeDefinition.Kind kind) {
        List<RecipeMatch<? extends RecipeDefinition>> result = new ArrayList<>();
        if (kind == RecipeDefinition.Kind.DISTILLATION) result.addAll(BuildCraftApiRuntime.INSTANCE.machineRecipes().recipes(DistillationRecipeDefinition.class));
        else result.addAll(BuildCraftApiRuntime.INSTANCE.machineRecipes().recipes(HeatExchangeRecipeDefinition.class).stream()
            .filter(match -> match.recipe().kind() == kind).toList());
        return result;
    }

    private static FluidIngredient inputOf(RecipeDefinition definition) {
        return definition instanceof DistillationRecipeDefinition d ? d.input() : ((HeatExchangeRecipeDefinition) definition).input();
    }

    private static RecipeDefinition toV2(RecipeDefinition.Kind kind, IRefineryRecipe recipe) {
        FluidStack canonicalInput = FluidCompatRegistry.canonicalize(recipe.in());
        FluidIngredient input = FluidIngredient.exact(FuelApiBridge.variantOf(canonicalInput), canonicalInput.getAmount());
        if (kind == RecipeDefinition.Kind.DISTILLATION) {
            IDistillationRecipe distillation = (IDistillationRecipe) recipe;
            return new DistillationRecipeDefinition(
                input, FuelApiBridge.volumeOf(distillation.outGas()), FuelApiBridge.volumeOf(distillation.outLiquid()), distillation.powerRequired()
            );
        }
        IHeatExchangerRecipe heat = (IHeatExchangerRecipe) recipe;
        FluidVolume output = heat.out() == null ? FluidVolume.empty() : FuelApiBridge.volumeOf(heat.out());
        return new HeatExchangeRecipeDefinition(kind, input, output, heat.heatFrom(), heat.heatTo());
    }

    private static FluidStack inputStack(FluidIngredient input) {
        return input.representativeVariant()
            .map(variant -> FuelApiBridge.stackOfVariant(variant, toLegacyAmount(input.amount().milliBuckets())))
            .orElse(FluidStack.EMPTY);
    }

    private static int toLegacyAmount(long amount) {
        if (amount > Integer.MAX_VALUE) throw new ArithmeticException("Legacy FluidStack cannot represent " + amount + " mB");
        return (int) amount;
    }

    private static ResourceLocation syntheticId(RecipeDefinition.Kind kind, FluidStack input) {
        FluidStack canonical = FluidCompatRegistry.canonicalize(input);
        ResourceLocation fluidId = FuelApiBridge.variantOf(canonical).fluidId();
        String suffix = canonical.getTag() == null ? "plain" : Integer.toUnsignedString(canonical.getTag().hashCode(), 16);
        String raw = "buildcraft:legacy_refinery/" + kind.name().toLowerCase() + "/" + fluidId.getNamespace() + "/" + fluidId.getPath() + "/" + suffix;
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) throw new IllegalArgumentException("Cannot create synthetic refinery recipe id from " + raw);
        return id;
    }

    public static abstract class RefineryRecipe implements IRefineryRecipe {
        private final FluidStack in;
        protected RefineryRecipe(FluidStack in) {
            if (in == null || in.isEmpty()) throw new IllegalArgumentException("Refinery input must not be empty");
            this.in = in.copy();
        }
        @Override public FluidStack in() { return in.copy(); }
    }

    public static class DistillationRecipe extends RefineryRecipe implements IDistillationRecipe {
        private final FluidStack outGas, outLiquid;
        private final long powerRequired;
        public DistillationRecipe(long powerRequired, FluidStack in, FluidStack outGas, FluidStack outLiquid) {
            super(in);
            if (powerRequired < 0) throw new IllegalArgumentException("powerRequired must be >= 0");
            this.powerRequired = powerRequired;
            this.outGas = outGas == null ? FluidStack.EMPTY : outGas.copy();
            this.outLiquid = outLiquid == null ? FluidStack.EMPTY : outLiquid.copy();
            if (this.outGas.isEmpty() && this.outLiquid.isEmpty()) throw new IllegalArgumentException("Distillation recipe must have an output");
        }
        @Override public FluidStack outGas() { return outGas.copy(); }
        @Override public FluidStack outLiquid() { return outLiquid.copy(); }
        @Override public long powerRequired() { return powerRequired; }
    }

    public static abstract class HeatExchangeRecipe extends RefineryRecipe implements IHeatExchangerRecipe {
        private final FluidStack out;
        private final int heatFrom, heatTo;
        protected HeatExchangeRecipe(FluidStack in, @Nullable FluidStack out, int heatFrom, int heatTo) {
            super(in);
            this.out = out == null ? null : out.copy();
            this.heatFrom = heatFrom;
            this.heatTo = heatTo;
        }
        @Override @Nullable public FluidStack out() { return out == null ? null : out.copy(); }
        @Override public int heatFrom() { return heatFrom; }
        @Override public int heatTo() { return heatTo; }
    }

    public static class HeatableRecipe extends HeatExchangeRecipe implements IHeatableRecipe {
        public HeatableRecipe(FluidStack in, @Nullable FluidStack out, int heatFrom, int heatTo) { super(in, out, heatFrom, heatTo); }
    }

    public static class CoolableRecipe extends HeatExchangeRecipe implements ICoolableRecipe {
        public CoolableRecipe(FluidStack in, @Nullable FluidStack out, int heatFrom, int heatTo) { super(in, out, heatFrom, heatTo); }
    }
}
