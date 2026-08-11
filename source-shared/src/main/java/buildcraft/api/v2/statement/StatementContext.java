package buildcraft.api.v2.statement;

import buildcraft.api.v2.OperationMode;
import buildcraft.api.v2.context.ExtensionContext;
import buildcraft.api.v2.permission.AutomationActor;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.Direction;

public record StatementContext(ExtensionContext views, AutomationActor actor, OperationMode mode, Direction side) {
    public StatementContext {
        Objects.requireNonNull(views, "views");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(mode, "mode");
    }
    public Optional<Direction> optionalSide() { return Optional.ofNullable(side); }
}
