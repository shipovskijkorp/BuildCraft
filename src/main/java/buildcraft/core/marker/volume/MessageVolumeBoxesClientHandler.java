/*
 * Client-only handler extracted from MessageVolumeBoxes so the dedicated server
 * never verifies/loads client classes (RuntimeDistCleaner dist-safety).
 */
package buildcraft.core.marker.volume;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import buildcraft.api.core.BCLog;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

@OnlyIn(Dist.CLIENT)
public class MessageVolumeBoxesClientHandler {
    public static void handle(MessageVolumeBoxes message, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                return;
            }
            Map<FriendlyByteBuf, VolumeBox> volumeBoxes = new LinkedHashMap<>();
            for (FriendlyByteBuf buffer : message.buffers) {
                try {
                    VolumeBox volumeBox = new VolumeBox(mc.level, buffer);
                    FriendlyByteBuf copy = new FriendlyByteBuf(Unpooled.buffer());
                    volumeBox.toBytes(copy);
                    volumeBoxes.put(copy, volumeBox);
                } catch (IOException | RuntimeException e) {
                    BCLog.logger.warn("Dropped invalid volume box packet", e);
                    return;
                }
            }

            ClientVolumeBoxes.INSTANCE.volumeBoxes.removeIf(volumeBox -> !volumeBoxes.values().contains(volumeBox));
            for (Map.Entry<FriendlyByteBuf, VolumeBox> entry : volumeBoxes.entrySet()) {
                boolean wasContained = false;
                for (VolumeBox clientVolumeBox : ClientVolumeBoxes.INSTANCE.volumeBoxes) {
                    if (clientVolumeBox.equals(entry.getValue())) {
                        try {
                            clientVolumeBox.fromBytes(entry.getKey());
                        } catch (IOException | RuntimeException io) {
                            BCLog.logger.warn("Dropped invalid volume box update", io);
                        }
                        wasContained = true;
                        break;
                    }
                }
                if (!wasContained) {
                    ClientVolumeBoxes.INSTANCE.volumeBoxes.add(entry.getValue());
                    for (Addon addon : entry.getValue().addons.values()) {
                        if (addon != null) {
                            addon.onAdded();
                        }
                    }
                }
            }
        });
    }
}
