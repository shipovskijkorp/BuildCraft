package buildcraft.api.v2.request;

import buildcraft.api.v2.item.ItemMatcher;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Stable request descriptor used by Requester blocks, docks and robot logistics. */
public record ItemRequest(ResourceLocation id, ItemMatcher matcher, int minimum, int maximum, int priority) {
    public ItemRequest {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(matcher, "matcher");
        if (minimum < 0) throw new IllegalArgumentException("minimum must be non-negative");
        if (maximum < minimum) throw new IllegalArgumentException("maximum must be >= minimum");
    }
}
