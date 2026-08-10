package buildcraft.api.v2.fuels;

import buildcraft.api.v2.fluid.FluidMatchContext;
import buildcraft.api.v2.fluid.FluidVariant;
import buildcraft.api.v2.reload.DefinitionSnapshot;
import buildcraft.api.v2.reload.ResolvedDefinition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Authoritative API 2 view of BuildCraft combustion fuels and coolants.
 *
 * Code registrations form a persistent baseline. {@link #reloadData} replaces
 * only reload-owned entries and publishes the combined view atomically.
 */
public interface EnergyFluidService {
    DefinitionSnapshot<EnergyFluidDefinition> snapshot();

    void register(ResourceLocation id, EnergyFluidDefinition definition, buildcraft.api.v2.reload.DefinitionProvenance provenance);

    EnergyFluidReloadResult reloadData(Collection<EnergyFluidRegistration> definitions);

    default Optional<ProfileMatch<FuelProfile>> findFuel(FluidVariant fluid, FluidMatchContext context) {
        return findFluidProfile(FuelProfile.class, fluid, context);
    }

    default Optional<ProfileMatch<CoolantProfile>> findCoolant(FluidVariant fluid, FluidMatchContext context) {
        return findFluidProfile(CoolantProfile.class, fluid, context);
    }

    default Optional<ProfileMatch<SolidCoolantProfile>> findSolidCoolant(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Optional.empty();
        List<ResolvedDefinition<EnergyFluidDefinition>> candidates = orderedDefinitions();
        for (ResolvedDefinition<EnergyFluidDefinition> resolved : candidates) {
            if (resolved.value() instanceof SolidCoolantProfile profile && profile.matches(stack)) {
                return Optional.of(new ProfileMatch<>(resolved.id(), profile, resolved.provenance()));
            }
        }
        return Optional.empty();
    }

    default List<ProfileMatch<FuelProfile>> fuels() {
        return profiles(FuelProfile.class);
    }

    default List<ProfileMatch<CoolantProfile>> coolants() {
        return profiles(CoolantProfile.class);
    }

    default List<ProfileMatch<SolidCoolantProfile>> solidCoolants() {
        return profiles(SolidCoolantProfile.class);
    }

    private <T> Optional<ProfileMatch<T>> findFluidProfile(
        Class<T> type, FluidVariant fluid, FluidMatchContext context
    ) {
        Objects.requireNonNull(fluid, "fluid");
        Objects.requireNonNull(context, "context");
        for (ResolvedDefinition<EnergyFluidDefinition> resolved : orderedDefinitions()) {
            EnergyFluidDefinition definition = resolved.value();
            boolean matches = definition instanceof FuelProfile fuel
                ? type == FuelProfile.class && fuel.matcher().matches(fluid, context)
                : definition instanceof CoolantProfile coolant
                && type == CoolantProfile.class && coolant.matches(fluid, context);
            if (matches) {
                return Optional.of(new ProfileMatch<>(resolved.id(), type.cast(definition), resolved.provenance()));
            }
        }
        return Optional.empty();
    }

    private <T> List<ProfileMatch<T>> profiles(Class<T> type) {
        List<ProfileMatch<T>> result = new ArrayList<>();
        for (ResolvedDefinition<EnergyFluidDefinition> resolved : orderedDefinitions()) {
            if (type.isInstance(resolved.value())) {
                result.add(new ProfileMatch<>(resolved.id(), type.cast(resolved.value()), resolved.provenance()));
            }
        }
        return List.copyOf(result);
    }

    /** Matching precedence is explicit priority, then stable definition id. */
    private List<ResolvedDefinition<EnergyFluidDefinition>> orderedDefinitions() {
        List<ResolvedDefinition<EnergyFluidDefinition>> definitions = new ArrayList<>(snapshot().definitions());
        definitions.sort(Comparator
            .<ResolvedDefinition<EnergyFluidDefinition>>comparingInt(value -> value.provenance().priority())
            .reversed()
            .thenComparing(value -> value.id().toString()));
        return definitions;
    }
}
