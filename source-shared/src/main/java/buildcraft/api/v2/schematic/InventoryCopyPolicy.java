package buildcraft.api.v2.schematic;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * Declarative policy for hidden/internal block inventory NBT that Builder may copy.
 * Empty blockIds means the policy applies globally; normally addons should target explicit block ids.
 */
public final class InventoryCopyPolicy {
    private final ResourceLocation id;
    private final Set<ResourceLocation> blockIds;
    private final Set<DataPath> allowedPaths;
    private final Set<DataPath> deniedPaths;

    /** Backward-compatible global policy constructor. */
    public InventoryCopyPolicy(ResourceLocation id, Set<DataPath> allowedPaths, Set<DataPath> deniedPaths) {
        this(id, Set.of(), allowedPaths, deniedPaths);
    }

    public InventoryCopyPolicy(
        ResourceLocation id,
        Set<ResourceLocation> blockIds,
        Set<DataPath> allowedPaths,
        Set<DataPath> deniedPaths
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.blockIds = Set.copyOf(Objects.requireNonNull(blockIds, "blockIds"));
        this.allowedPaths = Set.copyOf(Objects.requireNonNull(allowedPaths, "allowedPaths"));
        this.deniedPaths = Set.copyOf(Objects.requireNonNull(deniedPaths, "deniedPaths"));
    }

    public static Builder builder(ResourceLocation id) { return new Builder(id); }
    public static Builder forBlock(ResourceLocation id, ResourceLocation blockId) { return new Builder(id).block(blockId); }

    public ResourceLocation id() { return id; }
    public Set<ResourceLocation> blockIds() { return blockIds; }
    public Set<DataPath> allowedPaths() { return allowedPaths; }
    public Set<DataPath> deniedPaths() { return deniedPaths; }
    public boolean appliesTo(ResourceLocation blockId) { return blockIds.isEmpty() || blockIds.contains(blockId); }

    public static final class Builder {
        private final ResourceLocation id;
        private final Set<ResourceLocation> blockIds = new LinkedHashSet<>();
        private final Set<DataPath> allowed = new LinkedHashSet<>();
        private final Set<DataPath> denied = new LinkedHashSet<>();

        private Builder(ResourceLocation id) { this.id = Objects.requireNonNull(id, "id"); }
        public Builder block(ResourceLocation id) { blockIds.add(Objects.requireNonNull(id, "id")); return this; }
        public Builder allow(DataPath path) { allowed.add(Objects.requireNonNull(path, "path")); return this; }
        public Builder allow(String... path) { return allow(DataPath.of(path)); }
        public Builder deny(DataPath path) { denied.add(Objects.requireNonNull(path, "path")); return this; }
        public Builder deny(String... path) { return deny(DataPath.of(path)); }
        public InventoryCopyPolicy build() { return new InventoryCopyPolicy(id, blockIds, allowed, denied); }
    }
}
