package buildcraft.api.v2.machine;

import buildcraft.api.v2.persistence.OpaqueData;
import buildcraft.api.v2.persistence.PersistentType;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public record MachineComponentType<C extends MachineComponent>(ResourceLocation id, PersistentType<C, OpaqueData> persistence) {
    public MachineComponentType {
        Objects.requireNonNull(id, "id");
    }
    public Optional<PersistentType<C, OpaqueData>> optionalPersistence() { return Optional.ofNullable(persistence); }
}
