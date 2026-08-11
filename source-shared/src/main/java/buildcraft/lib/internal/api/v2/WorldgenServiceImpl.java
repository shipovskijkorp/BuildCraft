package buildcraft.lib.internal.api.v2;

import buildcraft.api.v2.registry.RegistrationContext;
import buildcraft.api.v2.worldgen.ResourceDepositRule;
import buildcraft.api.v2.worldgen.WorldgenService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/** Internal registry backend for BuildCraft resource-deposit extension rules. */
public final class WorldgenServiceImpl implements WorldgenService {
    private final Map<ResourceLocation, ResourceDepositRule> rules = new LinkedHashMap<>();
    private final Map<ResourceLocation, String> owners = new LinkedHashMap<>();

    @Override
    public synchronized void register(ResourceDepositRule rule, RegistrationContext context) {
        Objects.requireNonNull(rule, "rule");
        String owner = Objects.requireNonNull(context, "context").owner();
        if (owner == null || owner.isBlank()) throw new IllegalArgumentException("Worldgen registration owner must not be blank");
        ResourceDepositRule previous = rules.putIfAbsent(rule.id(), rule);
        if (previous != null) {
            throw new IllegalStateException("Duplicate worldgen rule " + rule.id() + " from " + owner
                + "; already owned by " + owners.get(rule.id()));
        }
        owners.put(rule.id(), owner);
    }

    @Override
    public synchronized Optional<ResourceDepositRule> rule(ResourceLocation id) {
        return Optional.ofNullable(rules.get(Objects.requireNonNull(id, "id")));
    }

    @Override
    public synchronized List<ResourceDepositRule> rules() {
        List<ResourceDepositRule> result = new ArrayList<>(rules.values());
        result.sort(Comparator.comparingInt(ResourceDepositRule::priority).reversed()
            .thenComparing(value -> value.id().toString()));
        return List.copyOf(result);
    }
}
