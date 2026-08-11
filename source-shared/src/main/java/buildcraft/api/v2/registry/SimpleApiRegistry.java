package buildcraft.api.v2.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** Small authoritative registry with provenance, aliases and an explicit freeze boundary. */
public final class SimpleApiRegistry<T> implements ApiRegistry<T> {
    private final Map<ResourceLocation, RegistryEntry<T>> entries = new LinkedHashMap<>();
    private final Map<ResourceLocation, ResourceLocation> aliases = new LinkedHashMap<>();
    private boolean frozen;

    @Override
    public void register(ResourceLocation id, T value) {
        register(id, value, () -> "unknown");
    }

    @Override
    public void register(ResourceLocation id, T value, RegistrationContext context) {
        ensureMutable();
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(context, "context");
        String owner = Objects.requireNonNull(context.owner(), "context.owner()");
        if (owner.isBlank()) throw new IllegalArgumentException("registration owner must not be blank");
        if (entries.containsKey(id) || aliases.containsKey(id)) {
            throw new IllegalStateException("Duplicate registry id or alias: " + id);
        }
        entries.put(id, new RegistryEntry<>(id, value, owner));
    }

    @Override
    public void registerAlias(ResourceLocation alias, ResourceLocation canonicalId, RegistrationContext context) {
        ensureMutable();
        Objects.requireNonNull(alias, "alias");
        Objects.requireNonNull(canonicalId, "canonicalId");
        Objects.requireNonNull(context, "context");
        if (alias.equals(canonicalId)) throw new IllegalArgumentException("alias must differ from canonicalId");
        if (entries.containsKey(alias) || aliases.putIfAbsent(alias, canonicalId) != null) {
            throw new IllegalStateException("Duplicate registry id or alias: " + alias);
        }
    }

    @Override
    public T get(ResourceLocation id) {
        RegistryEntry<T> entry = entries.get(canonicalId(id));
        return entry == null ? null : entry.value();
    }

    @Override
    public Optional<RegistryEntry<T>> entry(ResourceLocation id) {
        return Optional.ofNullable(entries.get(canonicalId(id)));
    }

    @Override
    public ResourceLocation canonicalId(ResourceLocation id) {
        Objects.requireNonNull(id, "id");
        ResourceLocation current = id;
        Set<ResourceLocation> visited = new LinkedHashSet<>();
        while (aliases.containsKey(current)) {
            if (!visited.add(current)) throw new IllegalStateException("Registry alias cycle starting at " + id);
            current = aliases.get(current);
        }
        return current;
    }

    @Override
    public Collection<T> values() {
        ArrayList<T> values = new ArrayList<>(entries.size());
        for (RegistryEntry<T> entry : entries.values()) values.add(entry.value());
        return Collections.unmodifiableList(values);
    }

    @Override
    public Collection<RegistryEntry<T>> entries() {
        return Collections.unmodifiableCollection(entries.values());
    }

    public Map<ResourceLocation, ResourceLocation> aliases() {
        return Collections.unmodifiableMap(aliases);
    }

    @Override public boolean frozen() { return frozen; }

    @Override
    public void freeze() {
        if (frozen) return;
        for (ResourceLocation alias : aliases.keySet()) {
            ResourceLocation canonical = canonicalId(alias);
            if (!entries.containsKey(canonical)) {
                throw new IllegalStateException("Registry alias " + alias + " targets missing id " + canonical);
            }
        }
        frozen = true;
    }

    private void ensureMutable() {
        if (frozen) throw new IllegalStateException("Registry is frozen");
    }
}
