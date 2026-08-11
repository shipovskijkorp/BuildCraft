package buildcraft.api.v2.map;

import buildcraft.api.v2.OperationMode;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

public interface MapLocationService {
    Optional<MapLocationAdapter> adapter(ItemStack stack);

    default Optional<MapLocationView> read(ItemStack stack) {
        return adapter(stack).flatMap(a -> a.read(stack));
    }

    default boolean write(ItemStack stack, MapLocationView location, OperationMode mode) {
        return adapter(stack).map(a -> a.write(stack, location, mode)).orElse(false);
    }

    default boolean clear(ItemStack stack, OperationMode mode) {
        return adapter(stack).map(a -> a.clear(stack, mode)).orElse(false);
    }
}
