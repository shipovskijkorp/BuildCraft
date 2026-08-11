package buildcraft.api.v2.statement;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record TriggerType(ResourceLocation id, ParameterSchema parameters, TriggerEvaluator evaluator) {
    public TriggerType {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(parameters, "parameters");
        Objects.requireNonNull(evaluator, "evaluator");
    }
}
