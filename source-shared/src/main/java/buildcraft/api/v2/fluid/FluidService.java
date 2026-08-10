package buildcraft.api.v2.fluid;

import buildcraft.api.v2.persistence.ApiCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * Common-facing fluid service. Native loader stack conversion is implemented
 * behind platform bridges and is deliberately not exposed in this contract.
 */
public interface FluidService extends FluidMatchContext {
    boolean isRegistered(ResourceLocation fluidId);

    default FluidVariant variant(ResourceLocation fluidId) {
        if (!isRegistered(fluidId)) {
            throw new IllegalArgumentException("Unknown fluid id: " + fluidId);
        }
        return FluidVariant.of(fluidId);
    }

    default FluidVariant variant(ResourceLocation fluidId, FluidComponentPayload components) {
        if (!isRegistered(fluidId)) {
            throw new IllegalArgumentException("Unknown fluid id: " + fluidId);
        }
        return FluidVariant.of(fluidId, components);
    }

    default ApiCodec<FluidVariant, FluidVariantData> variantCodec() {
        return FluidVariant.CODEC;
    }
}
