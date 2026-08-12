package buildcraft.api.v2.energy;

import java.util.Optional;
import net.minecraft.core.Direction;

/**
 * Loader-neutral way for a block entity or other runtime object to expose BuildCraft MJ ports.
 * <p>
 * Addon content can implement this interface directly instead of exposing Forge/NeoForge
 * capability types. BuildCraft's platform bridge may additionally discover native loader
 * endpoints and adapt them to the same {@link MjPort} contract.
 */
public interface MjPortProvider {
    Optional<MjPort> mjPort(Direction side);

    default Optional<MjPortDescriptor> mjPortDescriptor(Direction side) {
        return Optional.empty();
    }
}
