/*
 * Client-only handler extracted from MessageWireSystemsPowered for dist-safety.
 */
package buildcraft.transport.wire;

import java.util.Map;
import java.util.function.Supplier;

import buildcraft.api.transport.IWireManager;
import buildcraft.api.transport.pipe.IPipeHolder;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

@OnlyIn(Dist.CLIENT)
public class MessageWireSystemsPoweredClientHandler {
    public static void handle(MessageWireSystemsPowered message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Level level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            for (Map.Entry<Integer, Boolean> hashPowered : message.hashesPowered.entrySet()) {
                WireSystem system = ClientWireSystems.INSTANCE.wireSystems.get(hashPowered.getKey());
                if (system == null) {
                    continue;
                }
                boolean powered = hashPowered.getValue();
                for (WireSystem.WireElement element : system.elements) {
                    if (element.type != WireSystem.WireElement.Type.WIRE_PART) {
                        continue;
                    }
                    BlockEntity tile = level.getBlockEntity(element.blockPos);
                    if (tile instanceof IPipeHolder) {
                        IPipeHolder holder = (IPipeHolder) tile;
                        IWireManager iWireManager = holder.getWireManager();
                        if (iWireManager instanceof WireManager) {
                            WireManager wireManager = (WireManager) iWireManager;
                            if (wireManager.getColorOfPart(element.wirePart) != null) {
                                if (powered) {
                                    wireManager.poweredClient.add(element.wirePart);
                                } else {
                                    wireManager.poweredClient.remove(element.wirePart);
                                }
                            }
                        }
                    }
                }
            }
        });
    }
}
