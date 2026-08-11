package buildcraft.api.v2.map;

import buildcraft.api.v2.OperationMode;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

/** Adapter between an item stack and BuildCraft's semantic map-location data. */
public interface MapLocationAdapter {
    boolean supports(ItemStack stack);
    Optional<MapLocationView> read(ItemStack stack);
    boolean write(ItemStack stack, MapLocationView location, OperationMode mode);
    boolean clear(ItemStack stack, OperationMode mode);
}
