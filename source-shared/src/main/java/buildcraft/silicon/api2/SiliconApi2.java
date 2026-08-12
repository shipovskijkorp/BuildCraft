package buildcraft.silicon.api2;

import buildcraft.api.v2.BuildCraftServices;
import buildcraft.lib.internal.api.v2.BuildCraftApiRuntime;

/** One-time Silicon -> API2 runtime wiring. */
public final class SiliconApi2 {
    private static boolean installed;
    private SiliconApi2() {}

    public static synchronized void install() {
        if (installed) return;
        BuildCraftApiRuntime.bootstrap();
        BuildCraftApiRuntime.INSTANCE.installService(BuildCraftServices.GATES, GateServiceImpl.INSTANCE);
        installed = true;
    }
}
