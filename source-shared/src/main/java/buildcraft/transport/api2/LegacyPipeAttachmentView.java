package buildcraft.transport.api2;

import buildcraft.api.v2.pipe.PipeAttachment;
import buildcraft.transport.internal.pluggable.PipePluggable;
import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** API2 view over a BCCE internal pluggable. The pluggable implementation remains unsupported/internal. */
public final class LegacyPipeAttachmentView implements PipeAttachment {
    private final ResourceLocation typeId;
    private final Direction side;
    private final PipePluggable pluggable;

    public LegacyPipeAttachmentView(ResourceLocation typeId, Direction side, PipePluggable pluggable) {
        this.typeId = Objects.requireNonNull(typeId, "typeId");
        this.side = Objects.requireNonNull(side, "side");
        this.pluggable = Objects.requireNonNull(pluggable, "pluggable");
    }

    @Override public ResourceLocation typeId() { return typeId; }
    @Override public Direction side() { return side; }

    public PipePluggable internalPluggable() { return pluggable; }
}
