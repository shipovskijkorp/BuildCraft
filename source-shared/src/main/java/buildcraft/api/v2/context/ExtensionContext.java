package buildcraft.api.v2.context;

import java.util.Optional;

/** Read-only bag of explicitly published typed views for an operation. */
@FunctionalInterface
public interface ExtensionContext {
    <T> Optional<T> get(ContextKey<T> key);

    static ExtensionContext empty() {
        return new ExtensionContext() {
            @Override public <T> Optional<T> get(ContextKey<T> key) { return Optional.empty(); }
        };
    }
}
