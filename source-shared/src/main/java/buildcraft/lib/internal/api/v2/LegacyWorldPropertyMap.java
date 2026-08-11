package buildcraft.lib.internal.api.v2;

import buildcraft.api.core.IWorldProperty;
import buildcraft.api.v2.world.WorldProperty;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * Binary-compatible HashMap facade retained for BuildCraftAPI.worldProperties.
 * Mutations are mirrored into API 2, which is the authoritative lookup path.
 */
public final class LegacyWorldPropertyMap extends HashMap<String, IWorldProperty> {
    @Override
    public IWorldProperty put(String key, IWorldProperty value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        BuildCraftApiRuntime.INSTANCE.worldProperties().replaceLegacy(id(key), adapt(value));
        return super.put(key, value);
    }

    @Override
    public void putAll(Map<? extends String, ? extends IWorldProperty> values) {
        for (Map.Entry<? extends String, ? extends IWorldProperty> entry : values.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public IWorldProperty remove(Object key) {
        if (key instanceof String name) BuildCraftApiRuntime.INSTANCE.worldProperties().removeLegacy(id(name));
        return super.remove(key);
    }

    @Override
    public void clear() {
        for (String key : keySet().toArray(String[]::new)) {
            BuildCraftApiRuntime.INSTANCE.worldProperties().removeLegacy(id(key));
        }
        super.clear();
    }

    private static WorldProperty adapt(IWorldProperty property) {
        return new WorldProperty() {
            @Override public boolean test(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
                return property.get(level, pos);
            }
            @Override public void clear() { property.clear(); }
        };
    }

    private static ResourceLocation id(String name) {
        String candidate = name.indexOf(':') >= 0 ? name : "buildcraft:" + name;
        ResourceLocation parsed = ResourceLocation.tryParse(candidate);
        if (parsed != null) return parsed;

        // The legacy map accepted arbitrary String keys. Preserve them through a
        // deterministic escaped namespace rather than rejecting an old addon key.
        byte[] bytes = name.getBytes(StandardCharsets.UTF_8);
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) hex.append(String.format(java.util.Locale.ROOT, "%02x", value & 0xff));
        return Objects.requireNonNull(ResourceLocation.tryParse("buildcraft:legacy_world_property/" + hex));
    }
}
