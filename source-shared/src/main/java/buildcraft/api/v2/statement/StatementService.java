package buildcraft.api.v2.statement;

import java.util.Collection;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public interface StatementService {
    Optional<ParameterType<?>> parameterType(ResourceLocation id);
    Optional<TriggerType> triggerType(ResourceLocation id);
    Optional<ActionType> actionType(ResourceLocation id);
    Collection<ResourceLocation> availableTriggers(StatementContext context);
    Collection<ResourceLocation> availableActions(StatementContext context);
    boolean evaluateTrigger(ResourceLocation id, StatementContext context, StatementParameters parameters);
    StatementResult executeAction(ResourceLocation id, StatementContext context, StatementParameters parameters);
}
