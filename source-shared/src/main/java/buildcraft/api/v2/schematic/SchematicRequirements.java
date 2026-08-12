package buildcraft.api.v2.schematic;

import buildcraft.api.v2.fluid.FluidVolume;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.world.item.ItemStack;

/** Immutable resource requirements exposed by schematic elements. */
public final class SchematicRequirements {
    private static final SchematicRequirements EMPTY = new SchematicRequirements(List.of(), List.of());

    private final List<ItemStack> items;
    private final List<FluidVolume> fluids;

    public SchematicRequirements(List<ItemStack> items, List<FluidVolume> fluids) {
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(fluids, "fluids");
        List<ItemStack> itemCopies = new ArrayList<>(items.size());
        for (ItemStack stack : items) {
            if (stack != null && !stack.isEmpty()) itemCopies.add(stack.copy());
        }
        this.items = List.copyOf(itemCopies);
        this.fluids = List.copyOf(fluids);
    }

    public static SchematicRequirements empty() { return EMPTY; }
    public static SchematicRequirements items(List<ItemStack> items) { return new SchematicRequirements(items, List.of()); }
    public static SchematicRequirements fluids(List<FluidVolume> fluids) { return new SchematicRequirements(List.of(), fluids); }

    public List<ItemStack> items() {
        List<ItemStack> copies = new ArrayList<>(items.size());
        for (ItemStack stack : items) copies.add(stack.copy());
        return List.copyOf(copies);
    }

    public List<FluidVolume> fluids() { return fluids; }
    public boolean isEmpty() { return items.isEmpty() && fluids.isEmpty(); }
}
