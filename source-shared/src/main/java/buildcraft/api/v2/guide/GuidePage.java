package buildcraft.api.v2.guide;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * Loader-neutral page model for the BuildCraft Guide Book.
 *
 * <p>The standard page records are intentionally data-like. Addons should prefer these over
 * client render callbacks so the same content can be supplied from code or datapacks.
 */
public interface GuidePage {
    Kind kind();

    enum Kind {
        TEXT,
        RECIPE,
        IMAGE,
        LINK,
        ITEM
    }

    /** Markdown/plain text. When translatable is true, value is a translation key. */
    record Text(String value, boolean translatable) implements GuidePage {
        public Text {
            Objects.requireNonNull(value, "value");
            if (value.isBlank()) throw new IllegalArgumentException("Guide text must not be blank");
        }
        @Override public Kind kind() { return Kind.TEXT; }
    }

    record Recipe(ResourceLocation recipeId) implements GuidePage {
        public Recipe { Objects.requireNonNull(recipeId, "recipeId"); }
        @Override public Kind kind() { return Kind.RECIPE; }
    }

    record Image(ResourceLocation texture, int width, int height, String captionKey) implements GuidePage {
        public Image {
            Objects.requireNonNull(texture, "texture");
            if (width <= 0 || height <= 0) throw new IllegalArgumentException("Image dimensions must be positive");
        }
        @Override public Kind kind() { return Kind.IMAGE; }
    }

    record Link(ResourceLocation targetEntry, String labelKey) implements GuidePage {
        public Link {
            Objects.requireNonNull(targetEntry, "targetEntry");
            Objects.requireNonNull(labelKey, "labelKey");
        }
        @Override public Kind kind() { return Kind.LINK; }
    }

    record Item(ResourceLocation itemId, String textKey) implements GuidePage {
        public Item {
            Objects.requireNonNull(itemId, "itemId");
            Objects.requireNonNull(textKey, "textKey");
        }
        @Override public Kind kind() { return Kind.ITEM; }
    }
}
