package buildcraft.api.v2.automation;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record AutomationActionType<R extends AutomationRequest>(ResourceLocation id, Class<R> requestType, AutomationHandler<R> handler) {
    public AutomationActionType {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(requestType, "requestType");
        Objects.requireNonNull(handler, "handler");
    }
}
