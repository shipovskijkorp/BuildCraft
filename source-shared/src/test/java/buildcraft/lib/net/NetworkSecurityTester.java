package buildcraft.lib.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NetworkSecurityTester {
    @Test
    void boundedCountsRejectNegativeAndOversizedValues() {
        Assertions.assertEquals(4, NetworkSecurity.requireCount(4, 8, "count"));
        Assertions.assertThrows(DecoderException.class, () -> NetworkSecurity.requireCount(-1, 8, "count"));
        Assertions.assertThrows(DecoderException.class, () -> NetworkSecurity.requireCount(9, 8, "count"));
    }

    @Test
    void fullReadGuardRejectsTrailingFramingBytes() {
        ByteBuf buffer = Unpooled.buffer();
        try {
            NetworkSecurity.requireFullyRead(buffer, "empty");
            buffer.writeByte(1);
            Assertions.assertThrows(DecoderException.class,
                () -> NetworkSecurity.requireFullyRead(buffer, "test packet"));
        } finally {
            buffer.release();
        }
    }

    @Test
    void readableGuardRejectsTruncatedPayloads() {
        ByteBuf buffer = Unpooled.wrappedBuffer(new byte[] { 1, 2 });
        try {
            NetworkSecurity.requireReadable(buffer, 2, "payload");
            Assertions.assertThrows(DecoderException.class,
                () -> NetworkSecurity.requireReadable(buffer, 3, "payload"));
        } finally {
            buffer.release();
        }
    }
}
