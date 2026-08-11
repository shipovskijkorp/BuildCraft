package buildcraft.api.v2.pipe;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record PipeEventType<E>(ResourceLocation id, Class<E> eventType) {
    public PipeEventType {
        Objects.requireNonNull(id, "id"); Objects.requireNonNull(eventType, "eventType");
    }
}
