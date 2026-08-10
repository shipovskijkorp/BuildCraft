package buildcraft.api.v2.persistence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Loader-neutral result returned by API codecs and migrations.
 */
public final class CodecResult<T> {
    private final T value;
    private final List<String> errors;

    private CodecResult(T value, List<String> errors) {
        this.value = value;
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
    }

    public static <T> CodecResult<T> success(T value) {
        return new CodecResult<>(Objects.requireNonNull(value, "value"), List.of());
    }

    public static <T> CodecResult<T> failure(String error) {
        Objects.requireNonNull(error, "error");
        if (error.isBlank()) {
            throw new IllegalArgumentException("Codec error must not be blank");
        }
        return new CodecResult<>(null, List.of(error));
    }

    public static <T> CodecResult<T> failure(List<String> errors) {
        Objects.requireNonNull(errors, "errors");
        if (errors.isEmpty()) {
            throw new IllegalArgumentException("Failure requires at least one error");
        }
        List<String> checked = new ArrayList<>(errors.size());
        for (String error : errors) {
            Objects.requireNonNull(error, "error");
            if (error.isBlank()) {
                throw new IllegalArgumentException("Codec error must not be blank");
            }
            checked.add(error);
        }
        return new CodecResult<>(null, checked);
    }

    public boolean successful() {
        return value != null;
    }

    public Optional<T> value() {
        return Optional.ofNullable(value);
    }

    public T valueOrThrow() {
        if (value == null) {
            throw new IllegalStateException("Codec operation failed: " + String.join("; ", errors));
        }
        return value;
    }

    public List<String> errors() {
        return errors;
    }

    public <R> CodecResult<R> map(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        if (!successful()) {
            return CodecResult.failure(errors);
        }
        return CodecResult.success(mapper.apply(value));
    }
}
