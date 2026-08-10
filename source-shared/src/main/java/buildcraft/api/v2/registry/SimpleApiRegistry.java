package buildcraft.api.v2.registry;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

public final class SimpleApiRegistry<T> implements ApiRegistry<T> {
    private final Map<ResourceLocation, T> entries = new LinkedHashMap<>();
    private boolean frozen;

    @Override
    public void register(ResourceLocation id, T value) {
        if (frozen) {
            throw new IllegalStateException("Registry is frozen");
        }
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(value, "value");
        if (entries.putIfAbsent(id, value) != null) {
            throw new IllegalStateException("Duplicate registry id: " + id);
        }
    }

    @Override
    public T get(ResourceLocation id) {
        return entries.get(id);
    }

    @Override
    public Collection<T> values() {
        return Collections.unmodifiableCollection(entries.values());
    }

    @Override
    public boolean frozen() {
        return frozen;
    }

    @Override
    public void freeze() {
        frozen = true;
    }
}
