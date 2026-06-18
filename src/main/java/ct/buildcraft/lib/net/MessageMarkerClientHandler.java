package ct.buildcraft.lib.net;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MessageMarkerClientHandler {
    public static Level getClientLevel() {
        return Minecraft.getInstance().level;
    }
}
