package buildcraft.api.v2.registry;

import java.util.Collection;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/** Registration happens before freeze. After freeze this becomes immutable. */
public interface ApiRegistry<T> {
    void register(ResourceLocation id, T value);

    default void register(ResourceLocation id, T value, RegistrationContext context) {
        register(id, value);
    }

    default void registerAlias(ResourceLocation alias, ResourceLocation canonicalId, RegistrationContext context) {
        throw new UnsupportedOperationException("Aliases are not supported by this registry");
    }

    T get(ResourceLocation id);

    default Optional<RegistryEntry<T>> entry(ResourceLocation id) {
        T value = get(id);
        return value == null ? Optional.empty() : Optional.of(new RegistryEntry<>(canonicalId(id), value, "unknown"));
    }

    default ResourceLocation canonicalId(ResourceLocation id) {
        return id;
    }

    Collection<T> values();

    Collection<RegistryEntry<T>> entries();

    boolean frozen();
}
