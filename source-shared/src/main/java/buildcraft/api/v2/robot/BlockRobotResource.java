package buildcraft.api.v2.robot;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** A reservable block/side work target used to prevent multiple robots claiming the same job. */
public record BlockRobotResource(BlockPos position, Optional<Direction> side) implements RobotResource {
    public static final ResourceLocation TYPE = Objects.requireNonNull(ResourceLocation.tryParse("buildcraft:block_resource"));

    public BlockRobotResource {
        position = Objects.requireNonNull(position, "position").immutable();
        side = Objects.requireNonNull(side, "side");
    }

    public BlockRobotResource(BlockPos position) {
        this(position, Optional.empty());
    }

    @Override public ResourceLocation typeId() { return TYPE; }
}
