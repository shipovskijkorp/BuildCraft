package buildcraft.lib.net.cache;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NetworkedObjectCacheClientHandler {
    public static boolean isSameThread() {
        return Minecraft.getInstance().isSameThread();
    }
}
