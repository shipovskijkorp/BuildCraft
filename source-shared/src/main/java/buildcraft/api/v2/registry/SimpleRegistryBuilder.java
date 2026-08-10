package buildcraft.api.v2.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class SimpleRegistryBuilder<T> implements RegistryBuilder<T> {
    private final Map<ResourceLocation, RegistryEntry<T>> values = new LinkedHashMap<>();
    private RegistrySnapshot<T> snapshot;

    @Override
    public RegistryBuilder<T> register(ResourceLocation id, T value, RegistrationContext context) {
        if (snapshot != null) {
            throw new IllegalStateException("API registry is already frozen");
        }
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(context, "context");
        String owner = Objects.requireNonNull(context.owner(), "context.owner()");
        if (owner.isBlank()) {
            throw new IllegalArgumentException("Registration owner must not be blank");
        }
        if (values.containsKey(id)) {
            RegistryEntry<T> previous = values.get(id);
            throw new IllegalStateException(
                "Duplicate API registry id " + id + " from " + owner + "; already registered by " + previous.owner()
            );
        }
        values.put(id, new RegistryEntry<>(id, value, owner));
        return this;
    }

    @Override
    public RegistrySnapshot<T> freeze() {
        if (snapshot != null) {
            return snapshot;
        }
        Map<ResourceLocation, RegistryEntry<T>> frozen = Collections.unmodifiableMap(new LinkedHashMap<>(values));
        snapshot = new RegistrySnapshot<>() {
            @Override
            public Optional<T> get(ResourceLocation id) {
                RegistryEntry<T> entry = frozen.get(id);
                return entry == null ? Optional.empty() : Optional.of(entry.value());
            }

            @Override
            public Collection<RegistryEntry<T>> entries() {
                return frozen.values();
            }
        };
        return snapshot;
    }
}
