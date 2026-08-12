package buildcraft.lib.internal.mj;

import javax.annotation.Nonnull;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/** Loader-specific MJ capability tokens kept strictly inside BCCE Lib. */
public final class MjCapabilities {
    private MjCapabilities() {}

    @Nonnull public static final Capability<IMjConnector> CAP_CONNECTOR;
    @Nonnull public static final Capability<IMjReceiver> CAP_RECEIVER;
    @Nonnull public static final Capability<IMjRedstoneReceiver> CAP_REDSTONE_RECEIVER;
    @Nonnull public static final Capability<IMjReadable> CAP_READABLE;
    @Nonnull public static final Capability<IMjPassiveProvider> CAP_PASSIVE_PROVIDER;

    static {
        CAP_CONNECTOR = CapabilityManager.get(new CapabilityToken<>(){});
        CAP_RECEIVER = CapabilityManager.get(new CapabilityToken<>(){});
        CAP_REDSTONE_RECEIVER = CapabilityManager.get(new CapabilityToken<>(){});
        CAP_READABLE = CapabilityManager.get(new CapabilityToken<>(){});
        CAP_PASSIVE_PROVIDER = CapabilityManager.get(new CapabilityToken<>(){});
    }
}
