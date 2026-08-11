package buildcraft.api.v2.testkit;

import buildcraft.api.v2.context.ContextKey;
import buildcraft.api.v2.context.ExtensionContext;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class TestExtensionContext implements ExtensionContext {
    private final Map<ContextKey<?>, Object> values = new LinkedHashMap<>();

    public <T> TestExtensionContext put(ContextKey<T> key, T value) {
        if (!key.type().isInstance(value)) throw new IllegalArgumentException("Value does not match context key type " + key.type().getName());
        values.put(key, value);
        return this;
    }

    @Override
    public <T> Optional<T> get(ContextKey<T> key) {
        Object value = values.get(key);
        return value == null ? Optional.empty() : Optional.of(key.type().cast(value));
    }
}
