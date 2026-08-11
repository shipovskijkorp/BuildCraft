package buildcraft.api.v2.request;

import java.util.Collection;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public interface RequestService {
    Optional<RequestProvider> provider(Level level, BlockPos pos, Direction side);
    Collection<? extends RequestProvider> providers(Level level);
}
