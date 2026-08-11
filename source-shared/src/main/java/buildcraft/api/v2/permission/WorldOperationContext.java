package buildcraft.api.v2.permission;

import buildcraft.api.v2.OperationMode;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/** Complete immutable context for an automation permission query. */
public final class WorldOperationContext {
    private final AutomationActor actor;
    private final Level level;
    private final ResourceKey<Level> dimension;
    private final ResourceLocation dimensionId;
    private final BlockPos origin;
    private final WorldOperationTarget target;
    private final WorldOperationKind operation;
    private final OperationMode mode;
    private final ResourceLocation reasonId;

    public WorldOperationContext(
        AutomationActor actor,
        Level level,
        BlockPos origin,
        WorldOperationTarget target,
        WorldOperationKind operation,
        OperationMode mode,
        ResourceLocation reasonId
    ) {
        this(
            actor,
            Objects.requireNonNull(level, "level"),
            level.dimension(),
            level.dimension().location(),
            origin,
            target,
            operation,
            mode,
            reasonId
        );
    }

    private WorldOperationContext(
        AutomationActor actor,
        Level level,
        ResourceKey<Level> dimension,
        ResourceLocation dimensionId,
        BlockPos origin,
        WorldOperationTarget target,
        WorldOperationKind operation,
        OperationMode mode,
        ResourceLocation reasonId
    ) {
        this.actor = Objects.requireNonNull(actor, "actor");
        this.level = level;
        this.dimension = dimension;
        this.dimensionId = Objects.requireNonNull(dimensionId, "dimensionId");
        this.origin = Objects.requireNonNull(origin, "origin").immutable();
        this.target = Objects.requireNonNull(target, "target");
        this.operation = Objects.requireNonNull(operation, "operation");
        this.mode = Objects.requireNonNull(mode, "mode");
        this.reasonId = reasonId;
    }

    /**
     * Creates a context without constructing Minecraft's abstract Level class.
     * Package-private on purpose: this is only used by API unit tests.
     */
    static WorldOperationContext detachedForTesting(
        AutomationActor actor,
        ResourceLocation dimensionId,
        BlockPos origin,
        WorldOperationTarget target,
        WorldOperationKind operation,
        OperationMode mode,
        ResourceLocation reasonId
    ) {
        return new WorldOperationContext(actor, null, null, dimensionId, origin, target, operation, mode, reasonId);
    }

    public AutomationActor actor() { return actor; }

    public Level level() {
        if (level == null) {
            throw new IllegalStateException("Detached test permission context has no loaded Level");
        }
        return level;
    }

    public ResourceKey<Level> dimension() {
        if (dimension == null) {
            throw new IllegalStateException("Detached test permission context has no ResourceKey<Level>");
        }
        return dimension;
    }

    /** Stable dimension identifier that is also available to detached unit-test contexts. */
    public ResourceLocation dimensionId() { return dimensionId; }
    public BlockPos origin() { return origin; }
    public WorldOperationTarget target() { return target; }
    public WorldOperationKind operation() { return operation; }
    public OperationMode mode() { return mode; }
    public Optional<ResourceLocation> reasonId() { return Optional.ofNullable(reasonId); }

    public boolean isSimulation() {
        return mode == OperationMode.SIMULATE;
    }
}
