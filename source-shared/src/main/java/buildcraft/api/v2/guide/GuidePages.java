package buildcraft.api.v2.guide;

import net.minecraft.resources.ResourceLocation;

/** Short factories for the standard Guide Book page types. */
public final class GuidePages {
    private GuidePages() {
    }

    public static GuidePage.Text text(String markdown) {
        return new GuidePage.Text(markdown, false);
    }

    public static GuidePage.Text textKey(String translationKey) {
        return new GuidePage.Text(translationKey, true);
    }

    public static GuidePage.Recipe recipe(ResourceLocation recipeId) {
        return new GuidePage.Recipe(recipeId);
    }

    public static GuidePage.Image image(ResourceLocation texture, int width, int height) {
        return new GuidePage.Image(texture, width, height, null);
    }

    public static GuidePage.Image image(ResourceLocation texture, int width, int height, String captionKey) {
        return new GuidePage.Image(texture, width, height, captionKey);
    }

    public static GuidePage.Link link(ResourceLocation targetEntry, String labelKey) {
        return new GuidePage.Link(targetEntry, labelKey);
    }

    public static GuidePage.Item item(ResourceLocation itemId, String textKey) {
        return new GuidePage.Item(itemId, textKey);
    }
}
