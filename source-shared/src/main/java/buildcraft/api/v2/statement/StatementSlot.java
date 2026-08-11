package buildcraft.api.v2.statement;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public record StatementSlot(StatementKind kind, ResourceLocation statementId, Direction side, StatementParameters parameters) {
    public StatementSlot {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(statementId, "statementId");
        Objects.requireNonNull(parameters, "parameters");
    }
    public Optional<Direction> optionalSide() { return Optional.ofNullable(side); }
}
