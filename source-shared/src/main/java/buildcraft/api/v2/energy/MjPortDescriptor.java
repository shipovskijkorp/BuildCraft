package buildcraft.api.v2.energy;

import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * Immutable metadata describing how an MJ endpoint participates in a power network.
 * <p>
 * Descriptor discovery is allowed on either logical side and must stay side-effect free: building a descriptor must
 * not simulate or execute energy/item/fluid transfers. If a compatibility bridge cannot know a stable per-tick rate
 * without querying live transfer state, it should advertise {@link Long#MAX_VALUE} for the supported direction.
 */
public record MjPortDescriptor(
    ResourceLocation networkId,
    Set<MjPortRole> roles,
    MjAmount maxInsertPerTick,
    MjAmount maxExtractPerTick
) {
    public MjPortDescriptor {
        Objects.requireNonNull(networkId, "networkId");
        roles = Set.copyOf(Objects.requireNonNull(roles, "roles"));
        Objects.requireNonNull(maxInsertPerTick, "maxInsertPerTick");
        Objects.requireNonNull(maxExtractPerTick, "maxExtractPerTick");
    }

    public boolean has(MjPortRole role) {
        return roles.contains(role);
    }
}
