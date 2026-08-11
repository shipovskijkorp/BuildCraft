package buildcraft.api.v2.pipe;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public interface PipeAttachment {
    ResourceLocation typeId();
    Direction side();
}
