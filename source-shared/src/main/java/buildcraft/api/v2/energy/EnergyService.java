package buildcraft.api.v2.energy;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Runtime view of BuildCraft's MJ/external-energy policy and MJ endpoint lookup. */
public interface EnergyService {
    EnergyConversion conversion();
    boolean automaticFeConversionEnabled();
    boolean displayForgeEnergy();

    default EnergyConversionStatus status() {
        return new EnergyConversionStatus(conversion(), automaticFeConversionEnabled(), displayForgeEnergy(), EnergyRateUnit.PER_SECOND);
    }

    default Optional<MjPort> port(Level level, BlockPos pos, Direction side) {
        return Optional.empty();
    }

    default Optional<MjPortDescriptor> descriptor(Level level, BlockPos pos, Direction side) {
        return Optional.empty();
    }
}
