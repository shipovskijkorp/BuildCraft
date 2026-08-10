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
        this.actor = Objects.requireNonNull(actor, "actor");
        this.level = Objects.requireNonNull(level, "level");
        this.origin = Objects.requireNonNull(origin, "origin").immutable();
        this.target = Objects.requireNonNull(target, "target");
        this.operation = Objects.requireNonNull(operation, "operation");
        this.mode = Objects.requireNonNull(mode, "mode");
        this.reasonId = reasonId;
    }

    public AutomationActor actor() { return actor; }
    public Level level() { return level; }
    public ResourceKey<Level> dimension() { return level.dimension(); }
    public BlockPos origin() { return origin; }
    public WorldOperationTarget target() { return target; }
    public WorldOperationKind operation() { return operation; }
    public OperationMode mode() { return mode; }
    public Optional<ResourceLocation> reasonId() { return Optional.ofNullable(reasonId); }

    public boolean isSimulation() {
        return mode == OperationMode.SIMULATE;
    }
}
