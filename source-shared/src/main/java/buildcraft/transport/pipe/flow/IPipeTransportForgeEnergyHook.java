package buildcraft.transport.pipe.flow;

import net.minecraft.core.Direction;

/** Behaviour hook for Forge Energy pipe request/receive policy. */
public interface IPipeTransportForgeEnergyHook {
    /** @return amount consumed, or -1 to use the default receive behaviour. */
    int receivePower(Direction from, int value);

    /** @return adjusted FE request. */
    int requestPower(Direction from, int amount);
}
