package buildcraft.robotics.zone;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ZoneNetworkSecurityTester {
    @Test
    void oversizedZonePlanChunkCountIsRejected() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeInt(ZonePlan.MAX_SERIALIZED_CHUNKS + 1);
            buffer.readerIndex(0);
            Assertions.assertThrows(DecoderException.class, () -> new ZonePlan().readFromByteBuf(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void oversizedZoneChunkBitsetIsRejectedBeforeAllocation() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeByte(1);
            buffer.writeVarInt(33);
            buffer.readerIndex(0);
            Assertions.assertThrows(DecoderException.class, () -> new ZoneChunk().readFromByteBuf(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void unknownZoneChunkFlagsAreRejected() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(new byte[] { 4 }));
        try {
            Assertions.assertThrows(DecoderException.class, () -> new ZoneChunk().readFromByteBuf(buffer));
        } finally {
            buffer.release();
        }
    }
}
