package buildcraft.lib.internal.mj;

import buildcraft.api.v2.BuildCraftApi;
import buildcraft.api.v2.BuildCraftServices;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.common.util.LazyOptional;

/** Provides a quick way to return all types of a single {@link IMjConnector} for all the different capabilities. */
public class MjCapabilityHelper implements ICapabilityProvider {

    @Nonnull
    private final LazyOptional<IMjConnector> connector;

    @Nullable
    private final LazyOptional<IMjReceiver> receiver;

    @Nullable
    private final LazyOptional<IMjRedstoneReceiver> rsReceiver;

    @Nullable
    private final LazyOptional<IMjReadable> readable;

    @Nullable
    private final LazyOptional<IMjPassiveProvider> provider;

    @Nonnull
    private final LazyOptional<IEnergyStorage> feReceiver;

    public MjCapabilityHelper(@Nonnull IMjConnector mj) {
        this.connector = LazyOptional.of(() -> mj);
        this.receiver = mj instanceof IMjReceiver ? LazyOptional.of(() -> (IMjReceiver)mj) : LazyOptional.empty();
        this.rsReceiver = mj instanceof IMjRedstoneReceiver ? LazyOptional.of(() -> (IMjRedstoneReceiver)mj) : LazyOptional.empty();
        this.readable = mj instanceof IMjReadable ? LazyOptional.of(() -> (IMjReadable)mj) : LazyOptional.empty();
        this.provider = mj instanceof IMjPassiveProvider ? LazyOptional.of(() -> (IMjPassiveProvider)mj) : LazyOptional.empty();
        this.feReceiver = mj instanceof IMjReceiver ? LazyOptional.of(() -> new MjReceiverEnergyStorage((IMjReceiver) mj)) : LazyOptional.empty();
    }


    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, Direction facing) {
        if (capability == MjCapabilities.CAP_CONNECTOR) {
            return connector.cast();
        }
        if (capability == MjCapabilities.CAP_RECEIVER) {
            return receiver.cast();
        }
        if (capability == MjCapabilities.CAP_REDSTONE_RECEIVER) {
            return rsReceiver.cast();
        }
        if (capability == MjCapabilities.CAP_READABLE) {
            return readable.cast();
        }
        if (capability == MjCapabilities.CAP_PASSIVE_PROVIDER) {
            return provider.cast();
        }
        if (capability == ForgeCapabilities.ENERGY && BuildCraftApi.service(BuildCraftServices.ENERGY).automaticFeConversionEnabled()) {
            return feReceiver.cast();
        }
        return LazyOptional.empty();
    }
}
