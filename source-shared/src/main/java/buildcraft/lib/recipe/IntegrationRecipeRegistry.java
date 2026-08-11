/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */
package buildcraft.lib.recipe;

import buildcraft.api.recipes.IIntegrationRecipeRegistry;
import buildcraft.api.recipes.IngredientStack;
import buildcraft.api.recipes.IntegrationRecipe;
import buildcraft.api.v2.recipe.CountedIngredient;
import buildcraft.api.v2.recipe.IntegrationRecipeDefinition;
import buildcraft.api.v2.recipe.RecipeMatch;
import buildcraft.api.v2.reload.DefinitionProvenance;
import buildcraft.lib.internal.api.v2.BuildCraftApiRuntime;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import javax.annotation.Nonnull;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Legacy compatibility facade over the authoritative API 2 machine-recipe service.
 */
public final class IntegrationRecipeRegistry implements IIntegrationRecipeRegistry {
    public static final IntegrationRecipeRegistry INSTANCE = new IntegrationRecipeRegistry();
    private static final DefinitionProvenance LEGACY_PROVENANCE =
        new DefinitionProvenance("legacy-api", "integration-recipe", 0);

    private IntegrationRecipeRegistry() {}

    @Override
    public IntegrationRecipe getRecipeFor(@Nonnull ItemStack target, @Nonnull NonNullList<ItemStack> toIntegrate) {
        return BuildCraftApiRuntime.INSTANCE.machineRecipes()
            .findIntegration(target, toIntegrate)
            .map(IntegrationRecipeRegistry::legacyView)
            .orElse(null);
    }

    @Override
    public void addRecipe(IntegrationRecipe recipe) {
        if (recipe == null) throw new NullPointerException("recipe");
        BuildCraftApiRuntime.INSTANCE.machineRecipes().register(
            recipe.name,
            new LegacyDefinition(recipe),
            new DefinitionProvenance(LEGACY_PROVENANCE.owner(), "integration:" + recipe.name, LEGACY_PROVENANCE.priority())
        );
    }

    @Override
    public Iterable<IntegrationRecipe> getAllRecipes() {
        // The legacy API explicitly documents Iterator.remove(). Keep that behavior for
        // code-owned recipes while still exposing reload-owned API 2 recipes in the view.
        List<RecipeMatch<IntegrationRecipeDefinition>> snapshot =
            BuildCraftApiRuntime.INSTANCE.machineRecipes().recipes(IntegrationRecipeDefinition.class);
        return () -> new Iterator<>() {
            private int cursor;
            private RecipeMatch<IntegrationRecipeDefinition> last;
            private boolean canRemove;

            @Override
            public boolean hasNext() {
                return cursor < snapshot.size();
            }

            @Override
            public IntegrationRecipe next() {
                if (!hasNext()) throw new NoSuchElementException();
                last = snapshot.get(cursor++);
                canRemove = true;
                return legacyView(last);
            }

            @Override
            public void remove() {
                if (!canRemove) throw new IllegalStateException("next() must be called before remove()");
                if (!BuildCraftApiRuntime.INSTANCE.machineRecipes().removeCode(last.id())) {
                    throw new UnsupportedOperationException(
                        "Reload-owned API 2 recipe cannot be removed through the legacy iterator: " + last.id()
                    );
                }
                canRemove = false;
            }
        };
    }

    @Override
    public IntegrationRecipe getRecipe(@Nonnull ResourceLocation name) {
        return BuildCraftApiRuntime.INSTANCE.machineRecipes().snapshot().resolved(name)
            .filter(resolved -> resolved.value() instanceof IntegrationRecipeDefinition)
            .map(resolved -> legacyView(new RecipeMatch<>(
                resolved.id(), (IntegrationRecipeDefinition) resolved.value(), resolved.provenance()
            )))
            .orElse(null);
    }

    private static IntegrationRecipe legacyView(RecipeMatch<IntegrationRecipeDefinition> match) {
        IntegrationRecipeDefinition definition = match.recipe();
        return definition instanceof LegacyDefinition legacy ? legacy.delegate : new V2LegacyView(match.id(), definition);
    }

    private static final class LegacyDefinition implements IntegrationRecipeDefinition {
        private final IntegrationRecipe delegate;

        private LegacyDefinition(IntegrationRecipe delegate) {
            this.delegate = delegate;
        }

        @Override
        public ItemStack output(ItemStack target, NonNullList<ItemStack> toIntegrate) {
            return delegate.getOutput(target, toIntegrate);
        }

        @Override
        public List<CountedIngredient> requirements(ItemStack output) {
            ImmutableList<IngredientStack> requirements = delegate.getRequirements(output);
            List<CountedIngredient> converted = new ArrayList<>(requirements.size());
            for (IngredientStack stack : requirements) {
                converted.add(CountedIngredient.of(stack.ingredient, stack.count));
            }
            return List.copyOf(converted);
        }

        @Override
        public long requiredMicroJoules(ItemStack output) {
            return delegate.getRequiredMicroJoules(output);
        }

        @Override
        public CountedIngredient centerIngredient() {
            IngredientStack stack = delegate.getCenterStack();
            return CountedIngredient.of(stack.ingredient, stack.count);
        }
    }

    private static final class V2LegacyView extends IntegrationRecipe {
        private final IntegrationRecipeDefinition delegate;

        private V2LegacyView(ResourceLocation id, IntegrationRecipeDefinition delegate) {
            super(id);
            this.delegate = delegate;
        }

        @Override
        public ItemStack getOutput(@Nonnull ItemStack target, NonNullList<ItemStack> toIntegrate) {
            return delegate.output(target, toIntegrate);
        }

        @Override
        public ImmutableList<IngredientStack> getRequirements(@Nonnull ItemStack output) {
            ImmutableList.Builder<IngredientStack> builder = ImmutableList.builder();
            for (CountedIngredient ingredient : delegate.requirements(output)) {
                builder.add(new IngredientStack(ingredient.ingredient(), ingredient.count()));
            }
            return builder.build();
        }

        @Override
        public long getRequiredMicroJoules(ItemStack output) {
            return delegate.requiredMicroJoules(output);
        }

        @Override
        public IngredientStack getCenterStack() {
            CountedIngredient ingredient = delegate.centerIngredient();
            return new IngredientStack(ingredient.ingredient(), ingredient.count());
        }
    }
}
