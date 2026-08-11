package buildcraft.api.v2.list;

import java.util.List;
import java.util.Objects;
import net.minecraft.world.item.ItemStack;

public record ItemList(List<ItemListEntry> entries, ListMatchMode mode) {
    public ItemList {
        entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
        mode = Objects.requireNonNull(mode, "mode");
    }

    public boolean matches(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        long enabled = entries.stream().filter(ItemListEntry::enabled).count();
        if (enabled == 0) return mode == ListMatchMode.ALL || mode == ListMatchMode.NONE;
        long matches = entries.stream().filter(ItemListEntry::enabled).filter(entry -> entry.matcher().matches(stack)).count();
        return switch (mode) {
            case ANY -> matches > 0;
            case ALL -> matches == enabled;
            case NONE -> matches == 0;
        };
    }
}
