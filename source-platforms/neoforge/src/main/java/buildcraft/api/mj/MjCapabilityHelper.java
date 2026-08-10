package buildcraft.api.mj;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import buildcraft.api.capabilities.IBCCapabilityProvider;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;

/** Provides a quick way to expose all MJ interfaces implemented by one connector. */
public class MjCapabilityHelper implements IBCCapabilityProvider {
    @Nonnull
    private final IMjConnector connector;

    @Nullable
    private final IMjReceiver receiver;

    @Nullable
    private final IMjRedstoneReceiver rsReceiver;

    @Nullable
    private final IMjReadable readable;

    @Nullable
    private final IMjPassiveProvider provider;

    public MjCapabilityHelper(@Nonnull IMjConnector mj) {
        this.connector = mj;
        this.receiver = mj instanceof IMjReceiver value ? value : null;
        this.rsReceiver = mj instanceof IMjRedstoneReceiver value ? value : null;
        this.readable = mj instanceof IMjReadable value ? value : null;
        this.provider = mj instanceof IMjPassiveProvider value ? value : null;
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getCapability(BlockCapability<T, Direction> capability, @Nullable Direction facing) {
        if (capability == MjAPI.CAP_CONNECTOR) {
            return (T) connector;
        }
        if (capability == MjAPI.CAP_RECEIVER) {
            return (T) receiver;
        }
        if (capability == MjAPI.CAP_REDSTONE_RECEIVER) {
            return (T) rsReceiver;
        }
        if (capability == MjAPI.CAP_READABLE) {
            return (T) readable;
        }
        if (capability == MjAPI.CAP_PASSIVE_PROVIDER) {
            return (T) provider;
        }
        return null;
    }
}
