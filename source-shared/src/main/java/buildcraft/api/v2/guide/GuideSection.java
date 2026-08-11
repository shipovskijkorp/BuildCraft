package buildcraft.api.v2.guide;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/** One top-level or nested section in the BuildCraft Guide Book. */
public final class GuideSection {
    private final ResourceLocation id;
    private final String titleKey;
    private final ResourceLocation icon;
    private final ResourceLocation parent;
    private final int order;

    private GuideSection(Builder builder) {
        id = builder.id;
        titleKey = builder.titleKey;
        icon = builder.icon;
        parent = builder.parent;
        order = builder.order;
    }

    public static Builder builder(ResourceLocation id, String titleKey) {
        return new Builder(id, titleKey);
    }

    public ResourceLocation id() { return id; }
    public String titleKey() { return titleKey; }
    public Optional<ResourceLocation> icon() { return Optional.ofNullable(icon); }
    public Optional<ResourceLocation> parent() { return Optional.ofNullable(parent); }
    public int order() { return order; }

    public static final class Builder {
        private final ResourceLocation id;
        private final String titleKey;
        private ResourceLocation icon;
        private ResourceLocation parent;
        private int order;

        private Builder(ResourceLocation id, String titleKey) {
            this.id = Objects.requireNonNull(id, "id");
            this.titleKey = Objects.requireNonNull(titleKey, "titleKey");
            if (titleKey.isBlank()) throw new IllegalArgumentException("titleKey must not be blank");
        }

        public Builder icon(ResourceLocation icon) { this.icon = Objects.requireNonNull(icon, "icon"); return this; }
        public Builder parent(ResourceLocation parent) { this.parent = Objects.requireNonNull(parent, "parent"); return this; }
        public Builder order(int order) { this.order = order; return this; }
        public GuideSection build() { return new GuideSection(this); }
    }
}
