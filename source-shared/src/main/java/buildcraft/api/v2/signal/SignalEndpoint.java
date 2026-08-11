package buildcraft.api.v2.signal;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public record SignalEndpoint<T>(BlockPos position, Direction side, SignalChannelType<T> channel) {
    public SignalEndpoint {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(channel, "channel");
    }
}
