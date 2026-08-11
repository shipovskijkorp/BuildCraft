package buildcraft.api.v2.machine;

import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;

/**
 * Typed, stable machine setting that can be shared by built-in and addon machine definitions.
 *
 * <p>Properties describe configuration, not behaviour. Custom behaviour belongs in a
 * {@link MachineComponentType}; a property is appropriate for values such as work speed,
 * energy limits, range, capacity, or inventory size.
 */
public final class MachineProperty<T> {
    private final ResourceLocation id;
    private final Class<T> type;
    private final Predicate<? super T> validator;

    private MachineProperty(ResourceLocation id, Class<T> type, Predicate<? super T> validator) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    public static <T> MachineProperty<T> of(ResourceLocation id, Class<T> type) {
        return new MachineProperty<>(id, type, value -> true);
    }

    public static <T> MachineProperty<T> constrained(
        ResourceLocation id, Class<T> type, Predicate<? super T> validator
    ) {
        return new MachineProperty<>(id, type, validator);
    }

    public ResourceLocation id() {
        return id;
    }

    public Class<T> type() {
        return type;
    }

    public T validate(T value) {
        T checked = type.cast(Objects.requireNonNull(value, "value"));
        if (!validator.test(checked)) {
            throw new IllegalArgumentException("Invalid value for machine property " + id + ": " + checked);
        }
        return checked;
    }

    public T cast(Object value) {
        return validate(type.cast(value));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof MachineProperty<?> that && id.equals(that.id) && type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return 31 * id.hashCode() + type.hashCode();
    }

    @Override
    public String toString() {
        return "MachineProperty[" + id + ", " + type.getSimpleName() + "]";
    }
}
