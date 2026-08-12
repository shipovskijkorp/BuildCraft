package buildcraft.transport.api2;

import buildcraft.api.v2.BuildCraftApi;
import buildcraft.api.v2.BuildCraftRegistries;
import buildcraft.api.v2.pipe.PipeAttachmentType;
import buildcraft.api.v2.registry.ApiRegistry;
import buildcraft.transport.internal.pluggable.PluggableDefinition;
import buildcraft.transport.internal.pluggable.PipePluggable;
import buildcraft.transport.pipe.Pipe;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Mirrors internal pluggable definitions into the supported API2 attachment-type registry. */
public final class PipeAttachmentBridge {
    private PipeAttachmentBridge() {}

    public static void ensureRegistered(ResourceLocation id, PluggableDefinition definition) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(definition, "definition");
        ApiRegistry<PipeAttachmentType<?>> registry = BuildCraftApi.registry(BuildCraftRegistries.PIPE_ATTACHMENT_TYPES);
        if (registry.get(id) != null) return;

        PipeAttachmentType<LegacyPipeAttachmentView> type = new PipeAttachmentType<>(
            id,
            context -> {
                if (!(context.pipe() instanceof Pipe pipe)) {
                    throw new IllegalArgumentException("BCCE attachment placement requires a live BuildCraft pipe");
                }
                if (definition.creator == null) {
                    throw new IllegalStateException("Attachment " + id + " has no simple runtime creator");
                }
                PipePluggable pluggable = definition.creator.createSimplePluggable(
                    definition,
                    pipe.getHolder(),
                    context.side()
                );
                return new LegacyPipeAttachmentView(id, context.side(), pluggable);
            },
            null
        );
        registry.register(id, type, () -> id.getNamespace());
    }
}
