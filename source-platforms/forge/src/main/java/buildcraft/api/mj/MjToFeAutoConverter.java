package buildcraft.api.mj;

import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

/** Presents an FE receiver as an MJ receiver when automatic conversion is enabled. */
public final class MjToFeAutoConverter implements IMjReceiver, IMjReadable {
    private final IEnergyStorage fe;

    private MjToFeAutoConverter(IEnergyStorage fe) {
        this.fe = fe;
    }

    public static IMjReceiver createReceiver(IEnergyStorage fe) {
        if (fe == null || !fe.canReceive() || !MjAPI.isFeAutoConversionEnabled()) return null;
        return new MjToFeAutoConverter(fe);
    }

    @Override
    public boolean canConnect(IMjConnector other) {
        return true;
    }

    @Override
    public boolean canReceive() {
        return fe.canReceive() && MjAPI.isFeAutoConversionEnabled();
    }

    @Override
    public long getPowerRequested() {
        if (!canReceive()) return 0;
        // Simulation works for bufferless FE machines too; maxStored - stored does not.
        int simulated = Math.max(0, fe.receiveEnergy(Integer.MAX_VALUE, true));
        long requestedFe = simulated > 0
            ? simulated
            : Math.max(0L, (long) fe.getMaxEnergyStored() - fe.getEnergyStored());
        return MjAPI.getFeConversion().feToMicroMj(requestedFe);
    }

    @Override
    public long receivePower(long microJoules, FluidAction action) {
        if (!canReceive() || microJoules <= 0) return microJoules;
        long ratio = MjAPI.getFeConversion().mjPerFe;
        long convertible = microJoules / ratio;
        if (convertible <= 0) return microJoules;
        int offeredFe = (int) Math.min(Integer.MAX_VALUE, convertible);
        int acceptedFe = fe.receiveEnergy(offeredFe, action == FluidAction.SIMULATE);
        return microJoules - (long) acceptedFe * ratio;
    }

    @Override
    public long getStored() {
        return MjAPI.getFeConversion().feToMicroMj(fe.getEnergyStored());
    }

    @Override
    public long getCapacity() {
        return MjAPI.getFeConversion().feToMicroMj(fe.getMaxEnergyStored());
    }
}
