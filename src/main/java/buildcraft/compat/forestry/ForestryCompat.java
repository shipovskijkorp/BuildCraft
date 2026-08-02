package buildcraft.compat.forestry;

import buildcraft.api.lists.ListRegistry;

/** Optional Forestry Community Edition integration. Loaded reflectively by BCLib. */
public final class ForestryCompat {
    private ForestryCompat() {
    }

    public static void init() {
        ListRegistry.registerHandler(new ForestryListMatchHandler());
    }
}
