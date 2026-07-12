/*
 * Client-only handler for snapshot response packets.
 */
package buildcraft.builders.snapshot;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class MessageSnapshotResponseClientHandler {
    private MessageSnapshotResponseClientHandler() {
    }

    public static void handle(MessageSnapshotResponse message) {
        if (message.getSnapshot() != null) {
            ClientSnapshots.INSTANCE.onSnapshotReceived(message.getSnapshot());
        }
    }
}
