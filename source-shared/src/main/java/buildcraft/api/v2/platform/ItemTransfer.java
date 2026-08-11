package buildcraft.api.v2.platform;

import buildcraft.api.v2.item.ItemPort;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Platform bridge that exposes loader-native inventories as ItemPort. */
public interface ItemTransfer {
    Optional<ItemPort> find(Level level, BlockPos pos, Direction side);
}
