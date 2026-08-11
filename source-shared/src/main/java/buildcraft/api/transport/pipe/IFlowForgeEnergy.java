package buildcraft.api.transport.pipe;

import net.minecraft.core.Direction;

/**
 * Loader-neutral control surface for BuildCraft pipes that transport Forge Energy (FE).
 *
 * <p>The actual loader energy-storage type deliberately stays outside this public contract.</p>
 */
public interface IFlowForgeEnergy {
    /** Re-reads this pipe's transfer configuration and behaviour modifiers. */
    void reconfigure();

    /**
     * Returns the FE demand currently visible to this pipe. Passing {@code null}
     * asks for the aggregate network demand, matching the legacy gate trigger semantics.
     */
    int getPowerRequested(Direction side);

    /**
     * Attempts to pull FE from the external energy storage connected on {@code from}.
     *
     * @return the amount of FE accepted into the pipe.
     */
    int tryExtractPower(int maxExtracted, Direction from);

    /**
     * @return true when the external energy capability on this side is present and can receive FE.
     * Used by wooden/diamond-wood pipe presentation without exposing loader capability classes.
     */
    boolean isExternalEnergyReceiver(Direction side);
}
