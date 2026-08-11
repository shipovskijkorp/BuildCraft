package buildcraft.api.v2.content;

/** High-level entry point for addons that primarily want to add BuildCraft-style content. */
public final class BuildCraftContent {
    private BuildCraftContent() {
    }

    public static ContentRegistrar addon(String namespace) {
        return new ContentRegistrar(namespace, 0);
    }

    public static ContentRegistrar addon(String namespace, int priority) {
        return new ContentRegistrar(namespace, priority);
    }
}
