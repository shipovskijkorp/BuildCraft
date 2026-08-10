package buildcraft.api.v2.recipe;

import buildcraft.api.v2.fluid.FluidMatchContext;
import buildcraft.api.v2.fluid.FluidVariant;
import buildcraft.api.v2.reload.DefinitionProvenance;
import buildcraft.api.v2.reload.DefinitionSnapshot;
import buildcraft.api.v2.reload.ResolvedDefinition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Authoritative API 2 registry for BuildCraft machine recipes. */
public interface MachineRecipeService {
    DefinitionSnapshot<RecipeDefinition> snapshot();

    void register(ResourceLocation id, RecipeDefinition definition, DefinitionProvenance provenance);

    MachineRecipeReloadResult reloadData(Collection<MachineRecipeRegistration> definitions);

    default Optional<RecipeMatch<IntegrationRecipeDefinition>> findIntegration(
        ItemStack target, NonNullList<ItemStack> toIntegrate
    ) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(toIntegrate, "toIntegrate");
        for (ResolvedDefinition<RecipeDefinition> resolved : ordered()) {
            if (resolved.value() instanceof IntegrationRecipeDefinition recipe
                && !recipe.output(target, toIntegrate).isEmpty()) {
                return Optional.of(match(resolved, recipe));
            }
        }
        return Optional.empty();
    }

    default Optional<RecipeMatch<DistillationRecipeDefinition>> findDistillation(
        FluidVariant input, FluidMatchContext context
    ) {
        return findFluid(DistillationRecipeDefinition.class, RecipeDefinition.Kind.DISTILLATION, input, context);
    }

    default Optional<RecipeMatch<HeatExchangeRecipeDefinition>> findHeating(
        FluidVariant input, FluidMatchContext context
    ) {
        return findFluid(HeatExchangeRecipeDefinition.class, RecipeDefinition.Kind.HEATING, input, context);
    }

    default Optional<RecipeMatch<HeatExchangeRecipeDefinition>> findCooling(
        FluidVariant input, FluidMatchContext context
    ) {
        return findFluid(HeatExchangeRecipeDefinition.class, RecipeDefinition.Kind.COOLING, input, context);
    }

    default <T extends RecipeDefinition> List<RecipeMatch<T>> recipes(Class<T> type) {
        List<RecipeMatch<T>> result = new ArrayList<>();
        for (ResolvedDefinition<RecipeDefinition> resolved : ordered()) {
            if (type.isInstance(resolved.value())) result.add(match(resolved, type.cast(resolved.value())));
        }
        return List.copyOf(result);
    }

    private <T extends RecipeDefinition> Optional<RecipeMatch<T>> findFluid(
        Class<T> type, RecipeDefinition.Kind kind, FluidVariant input, FluidMatchContext context
    ) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(context, "context");
        for (ResolvedDefinition<RecipeDefinition> resolved : ordered()) {
            RecipeDefinition value = resolved.value();
            if (value.kind() != kind || !type.isInstance(value)) continue;
            FluidIngredient ingredient = value instanceof DistillationRecipeDefinition distillation
                ? distillation.input() : ((HeatExchangeRecipeDefinition) value).input();
            if (ingredient.matches(input, context)) return Optional.of(match(resolved, type.cast(value)));
        }
        return Optional.empty();
    }

    private List<ResolvedDefinition<RecipeDefinition>> ordered() {
        List<ResolvedDefinition<RecipeDefinition>> definitions = new ArrayList<>(snapshot().definitions());
        definitions.sort(Comparator
            .<ResolvedDefinition<RecipeDefinition>>comparingInt(v -> v.provenance().priority()).reversed()
            .thenComparing(v -> v.id().toString()));
        return definitions;
    }

    private static <T extends RecipeDefinition> RecipeMatch<T> match(
        ResolvedDefinition<RecipeDefinition> resolved, T recipe
    ) {
        return new RecipeMatch<>(resolved.id(), recipe, resolved.provenance());
    }
}
