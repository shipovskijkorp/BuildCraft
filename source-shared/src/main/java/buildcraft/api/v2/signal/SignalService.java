package buildcraft.api.v2.signal;

import java.util.Collection;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public interface SignalService {
    Optional<SignalPort<?>> port(Level level, BlockPos pos, Direction side, ResourceLocation channelId);
    Collection<? extends SignalNetworkView<?>> networks(Level level);
}
