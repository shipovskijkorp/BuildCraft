package buildcraft.api.v2.statement;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record ActionType(ResourceLocation id, ParameterSchema parameters, ActionExecutor executor) {
    public ActionType {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(parameters, "parameters");
        Objects.requireNonNull(executor, "executor");
    }
}
