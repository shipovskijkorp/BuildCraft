package buildcraft.api.v2.registry;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;

/**
 * Registration happens before freeze. After freeze this becomes immutable.
 */
public interface ApiRegistry<T> {
    void register(ResourceLocation id, T value);

    T get(ResourceLocation id);

    Collection<T> values();

    boolean frozen();

    void freeze();
}
