/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.net;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import buildcraft.api.core.BCLog;
import buildcraft.lib.gui.MenuBC_Neptune;
import buildcraft.lib.misc.MessageUtil;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

public class MessageContainer {

    private static final int MAX_PAYLOAD_SIZE = 32 * 1024;

    private int windowId;
    private int msgId;
    private FriendlyByteBuf payload;

    @SuppressWarnings("unused")
    public MessageContainer() {}

    public MessageContainer(int windowId, int msgId, FriendlyByteBuf payload) {
        this.windowId = windowId;
        this.msgId = msgId;
        this.payload = payload;
    }

    // Packet breakdown:
    // INT - WindowId
    // USHORT - MsgId
    // USHORT - PAYLOAD_SIZE->"size"
    // BYTE[size] - PAYLOAD

    public MessageContainer(FriendlyByteBuf buf) {
        windowId = buf.readInt();
        msgId = buf.readUnsignedShort();
        int payloadSize = buf.readUnsignedShort();
        if (payloadSize > MAX_PAYLOAD_SIZE || payloadSize > buf.readableBytes()) {
            throw new DecoderException("Invalid container packet payload size: " + payloadSize
                + " readable=" + buf.readableBytes());
        }
        payload = new FriendlyByteBuf(buf.readBytes(payloadSize));
    }

    public static void toBytes(MessageContainer msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.windowId);
        buf.writeShort(msg.msgId);
        int length = msg.payload.readableBytes();
        if (length > MAX_PAYLOAD_SIZE) {
            throw new IllegalStateException("Container packet payload is too large: " + length);
        }
        buf.writeShort(length);
        buf.writeBytes(msg.payload, 0, length);
    }

    public static final BiConsumer<MessageContainer, Supplier<NetworkEvent.Context>> HANDLER = (message, ctx) -> {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            try {
                int id = message.windowId;
                ServerPlayer player = context.getSender();
                LogicalSide side = context.getDirection().getReceptionSide();
                if (player != null && player.containerMenu.containerId == id
                    && player.containerMenu instanceof MenuBC_Neptune container) {

                    container.readMessage(message.msgId, message.payload, side, context);
                    String extra = container.getClass() + ", id = "
                        + container.getIdAllocator().getNameFor(message.msgId);
                    MessageUtil.ensureEmpty(message.payload, side == LogicalSide.CLIENT, extra);
                } else if (side == LogicalSide.CLIENT
                    && MessageContainerClientHandler.getClientContainerMenu() instanceof MenuBC_Neptune container) {
                    container.readMessage(message.msgId, message.payload, side, context);
                    String extra = container.getClass() + ", id = "
                        + container.getIdAllocator().getNameFor(message.msgId);
                    MessageUtil.ensureEmpty(message.payload, true, extra);
                }
            } catch (IOException | RuntimeException e) {
                BCLog.logger.warn("Dropped invalid BuildCraft container packet", e);
            } finally {
                // message.payload.release();
            }
        });
        context.setPacketHandled(true);
    };
}
