package buildcraft.lib.internal.api.v2;

import buildcraft.api.v2.crops.CropAdapter;
import buildcraft.api.v2.crops.CropRegistration;
import buildcraft.api.v2.crops.CropService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public final class CropServiceImpl implements CropService {
    private final Map<ResourceLocation, CropRegistration> registrations = new LinkedHashMap<>();
    private volatile List<CropRegistration> snapshot = List.of();

    @Override
    public synchronized void register(ResourceLocation id, int priority, CropAdapter adapter) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(adapter, "adapter");
        if (registrations.containsKey(id)) throw new IllegalStateException("Duplicate crop adapter id: " + id);
        registrations.put(id, new CropRegistration(id, priority, adapter));
        rebuild();
    }

    @Override public List<CropRegistration> adapters() { return snapshot; }

    public synchronized void replaceLegacy(ResourceLocation id, int priority, CropAdapter adapter) {
        registrations.put(Objects.requireNonNull(id, "id"), new CropRegistration(id, priority, Objects.requireNonNull(adapter, "adapter")));
        rebuild();
    }

    public synchronized void removeLegacy(ResourceLocation id) {
        if (registrations.remove(id) != null) rebuild();
    }

    private void rebuild() {
        List<CropRegistration> ordered = new ArrayList<>(registrations.values());
        ordered.sort(Comparator.comparingInt(CropRegistration::priority).reversed().thenComparing(e -> e.id().toString()));
        snapshot = List.copyOf(ordered);
    }
}
