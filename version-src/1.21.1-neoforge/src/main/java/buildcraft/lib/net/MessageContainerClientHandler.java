package buildcraft.lib.net;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MessageContainerClientHandler {
    public static AbstractContainerMenu getClientContainerMenu() {
        return Minecraft.getInstance().player.containerMenu;
    }
}
