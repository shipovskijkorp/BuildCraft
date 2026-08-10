package buildcraft.api.v2.platform;

import java.util.Optional;

public interface PlatformServices {
    Optional<ItemTransfer> itemTransfer();
    Optional<FluidTransfer> fluidTransfer();
    Optional<EnergyTransfer> energyTransfer();
}
