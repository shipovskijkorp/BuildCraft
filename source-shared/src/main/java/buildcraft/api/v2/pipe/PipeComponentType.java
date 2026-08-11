package buildcraft.api.v2.pipe;

import buildcraft.api.v2.persistence.OpaqueData;
import buildcraft.api.v2.persistence.PersistentType;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class PipeComponentType<C extends PipeComponent> {
    private final ResourceLocation id;
    private final PipeComponentFactory<C> factory;
    private final PersistentType<C, OpaqueData> persistence;

    public PipeComponentType(ResourceLocation id, PipeComponentFactory<C> factory, PersistentType<C, OpaqueData> persistence) {
        this.id = Objects.requireNonNull(id, "id");
        this.factory = Objects.requireNonNull(factory, "factory");
        this.persistence = persistence;
    }
    public ResourceLocation id() { return id; }
    public C create(PipeView pipe) { return factory.create(pipe); }
    public Optional<PersistentType<C, OpaqueData>> persistence() { return Optional.ofNullable(persistence); }
}
