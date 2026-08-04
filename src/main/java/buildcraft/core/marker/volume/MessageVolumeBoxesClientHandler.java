/*
 * Client-only handler extracted from MessageVolumeBoxes so the dedicated server
 * never verifies/loads client classes (RuntimeDistCleaner dist-safety).
 */
package buildcraft.core.marker.volume;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
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
    private record DecodedVolumeBox(FriendlyByteBuf buffer, VolumeBox box) {}

    public static void handle(MessageVolumeBoxes message, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                return;
            }
            Map<UUID, DecodedVolumeBox> updates = new LinkedHashMap<>();
            for (FriendlyByteBuf buffer : message.buffers) {
                try {
                    VolumeBox volumeBox = new VolumeBox(mc.level, buffer);
                    FriendlyByteBuf copy = new FriendlyByteBuf(Unpooled.buffer());
                    volumeBox.toBytes(copy);
                    updates.put(volumeBox.id, new DecodedVolumeBox(copy, volumeBox));
                } catch (IOException | RuntimeException e) {
                    BCLog.logger.warn("Dropped invalid volume box packet", e);
                    return;
                }
            }

            if (message.replaceAll) {
                ClientVolumeBoxes.INSTANCE.volumeBoxes.removeIf(volumeBox -> !updates.containsKey(volumeBox.id));
            } else if (!message.removedIds.isEmpty()) {
                ClientVolumeBoxes.INSTANCE.volumeBoxes.removeIf(volumeBox -> message.removedIds.contains(volumeBox.id));
            }

            for (Map.Entry<UUID, DecodedVolumeBox> entry : updates.entrySet()) {
                VolumeBox existing = ClientVolumeBoxes.INSTANCE.volumeBoxes.stream()
                    .filter(volumeBox -> volumeBox.id.equals(entry.getKey()))
                    .findFirst()
                    .orElse(null);
                if (existing != null) {
                    try {
                        existing.fromBytes(entry.getValue().buffer());
                    } catch (IOException | RuntimeException io) {
                        BCLog.logger.warn("Dropped invalid volume box update", io);
                    }
                    continue;
                }

                VolumeBox added = entry.getValue().box();
                ClientVolumeBoxes.INSTANCE.volumeBoxes.add(added);
                for (Addon addon : added.addons.values()) {
                    if (addon != null) {
                        addon.onAdded();
                    }
                }
            }
        });
    }
}
