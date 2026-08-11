package buildcraft.api.v2.pipe;

import net.minecraft.resources.ResourceLocation;

/** Scoped mutation operations granted while a component callback is executing. */
public interface PipeMutationContext extends PipeView {
    void markChanged();
    void requestSync(ResourceLocation channelId);
}
