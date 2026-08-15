package buildcraft.lib.net;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PacketBufferBCTester {
    private enum ThreeValues {
        FIRST,
        SECOND,
        THIRD
    }

    @Test
    void invalidPackedEnumOrdinalIsRejected() {
        PacketBufferBC buffer = new PacketBufferBC(Unpooled.wrappedBuffer(new byte[] { 0x03 }));
        try {
            Assertions.assertThrows(DecoderException.class, () -> buffer.readEnum(ThreeValues.class));
        } finally {
            buffer.release();
        }
    }

    @Test
    void oversizedLegacyStringIsRejectedBeforeAllocation() {
        PacketBufferBC buffer = new PacketBufferBC(Unpooled.buffer());
        try {
            buffer.writeVarInt(32 * 1024 + 1);
            buffer.readerIndex(0);
            Assertions.assertThrows(DecoderException.class, buffer::readString);
        } finally {
            buffer.release();
        }
    }

    @Test
    void truncatedLegacyStringIsRejected() {
        PacketBufferBC buffer = new PacketBufferBC(Unpooled.buffer());
        try {
            buffer.writeVarInt(4);
            buffer.writeByte('a');
            buffer.readerIndex(0);
            Assertions.assertThrows(DecoderException.class, buffer::readString);
        } finally {
            buffer.release();
        }
    }
}
