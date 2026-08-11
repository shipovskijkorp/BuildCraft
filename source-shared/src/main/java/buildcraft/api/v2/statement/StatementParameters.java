package buildcraft.api.v2.statement;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class StatementParameters {
    public static final StatementParameters EMPTY = new StatementParameters(Map.of());
    private final Map<ResourceLocation, ParameterValue<?>> values;

    public StatementParameters(Map<ResourceLocation, ParameterValue<?>> values) {
        this.values = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNull(values, "values")));
    }

    public Map<ResourceLocation, ParameterValue<?>> values() { return values; }

    public <T> Optional<T> get(ResourceLocation slotId, ParameterType<T> type) {
        ParameterValue<?> value = values.get(slotId);
        if (value == null || !value.type().id().equals(type.id())) return Optional.empty();
        @SuppressWarnings("unchecked") T cast = (T) value.value();
        return Optional.of(cast);
    }
}
