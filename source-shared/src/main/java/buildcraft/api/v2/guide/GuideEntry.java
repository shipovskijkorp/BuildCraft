package buildcraft.api.v2.guide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/** Immutable Guide Book entry supplied by BuildCraft or an addon. */
public final class GuideEntry {
    private final ResourceLocation id;
    private final ResourceLocation section;
    private final String titleKey;
    private final ResourceLocation icon;
    private final int order;
    private final List<GuidePage> pages;

    private GuideEntry(Builder builder) {
        id = builder.id;
        section = builder.section;
        titleKey = builder.titleKey;
        icon = builder.icon;
        order = builder.order;
        pages = Collections.unmodifiableList(new ArrayList<>(builder.pages));
    }

    public static Builder builder(ResourceLocation id, ResourceLocation section, String titleKey) {
        return new Builder(id, section, titleKey);
    }

    public ResourceLocation id() { return id; }
    public ResourceLocation section() { return section; }
    public String titleKey() { return titleKey; }
    public Optional<ResourceLocation> icon() { return Optional.ofNullable(icon); }
    public int order() { return order; }
    public List<GuidePage> pages() { return pages; }

    public static final class Builder {
        private final ResourceLocation id;
        private final ResourceLocation section;
        private final String titleKey;
        private ResourceLocation icon;
        private int order;
        private final List<GuidePage> pages = new ArrayList<>();

        private Builder(ResourceLocation id, ResourceLocation section, String titleKey) {
            this.id = Objects.requireNonNull(id, "id");
            this.section = Objects.requireNonNull(section, "section");
            this.titleKey = Objects.requireNonNull(titleKey, "titleKey");
            if (titleKey.isBlank()) throw new IllegalArgumentException("titleKey must not be blank");
        }

        public Builder icon(ResourceLocation icon) { this.icon = Objects.requireNonNull(icon, "icon"); return this; }
        public Builder order(int order) { this.order = order; return this; }
        public Builder page(GuidePage page) { pages.add(Objects.requireNonNull(page, "page")); return this; }
        public Builder pages(List<? extends GuidePage> pages) {
            Objects.requireNonNull(pages, "pages").forEach(this::page);
            return this;
        }
        public GuideEntry build() {
            if (pages.isEmpty()) throw new IllegalStateException("Guide entry must contain at least one page");
            return new GuideEntry(this);
        }
    }
}
