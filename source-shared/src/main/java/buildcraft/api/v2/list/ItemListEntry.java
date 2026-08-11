package buildcraft.api.v2.list;

import buildcraft.api.v2.item.ItemMatcher;
import java.util.Objects;

public record ItemListEntry(ItemMatcher matcher, boolean enabled) {
    public ItemListEntry {
        Objects.requireNonNull(matcher, "matcher");
    }
}
