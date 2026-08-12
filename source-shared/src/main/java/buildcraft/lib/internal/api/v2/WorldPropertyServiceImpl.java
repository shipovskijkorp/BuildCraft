package buildcraft.lib.internal.api.v2;

import buildcraft.api.v2.world.WorldProperty;
import buildcraft.api.v2.world.WorldPropertyService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class WorldPropertyServiceImpl implements WorldPropertyService {
    private final Map<ResourceLocation, WorldProperty> properties = new LinkedHashMap<>();
    private volatile Map<ResourceLocation, WorldProperty> snapshot = Map.of();

    @Override
    public synchronized void register(ResourceLocation id, WorldProperty property) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(property, "property");
        if (properties.containsKey(id)) throw new IllegalStateException("Duplicate world property id: " + id);
        properties.put(id, property);
        snapshot = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
    }

    @Override public Optional<WorldProperty> get(ResourceLocation id) { return Optional.ofNullable(snapshot.get(id)); }
    @Override public Map<ResourceLocation, WorldProperty> properties() { return snapshot; }
}
