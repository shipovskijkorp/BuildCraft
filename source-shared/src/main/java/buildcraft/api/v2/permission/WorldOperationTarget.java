package buildcraft.api.v2.permission;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;

/** Optional block/entity target for one permission decision. */
public final class WorldOperationTarget {
    private static final WorldOperationTarget NONE = new WorldOperationTarget(null, null);

    private final BlockPos blockPos;
    private final UUID entityId;

    private WorldOperationTarget(BlockPos blockPos, UUID entityId) {
        this.blockPos = blockPos == null ? null : blockPos.immutable();
        this.entityId = entityId;
    }

    public static WorldOperationTarget none() {
        return NONE;
    }

    public static WorldOperationTarget block(BlockPos pos) {
        if (pos == null) throw new NullPointerException("pos");
        return new WorldOperationTarget(pos, null);
    }

    public static WorldOperationTarget entity(UUID entityId) {
        if (entityId == null) throw new NullPointerException("entityId");
        return new WorldOperationTarget(null, entityId);
    }

    public static WorldOperationTarget blockAndEntity(BlockPos pos, UUID entityId) {
        if (pos == null) throw new NullPointerException("pos");
        if (entityId == null) throw new NullPointerException("entityId");
        return new WorldOperationTarget(pos, entityId);
    }

    public Optional<BlockPos> blockPos() {
        return Optional.ofNullable(blockPos);
    }

    public Optional<UUID> entityId() {
        return Optional.ofNullable(entityId);
    }
}
