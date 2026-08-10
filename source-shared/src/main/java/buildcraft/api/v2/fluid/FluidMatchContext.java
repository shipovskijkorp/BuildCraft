package buildcraft.api.v2.fluid;

import net.minecraft.resources.ResourceLocation;

/** Loader-neutral lookup context supplied when evaluating a fluid matcher. */
@FunctionalInterface
public interface FluidMatchContext {
    boolean isInTag(ResourceLocation fluidId, ResourceLocation tagId);
}
