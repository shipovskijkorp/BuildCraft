/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.net;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import buildcraft.api.core.BCLog;
import buildcraft.lib.misc.MessageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import io.netty.handler.codec.DecoderException;

public class MessageUpdateTile {
    private static final int MAX_PAYLOAD_SIZE = 1024 * 1024;

    private BlockPos pos;
    private FriendlyByteBuf payload;

    @SuppressWarnings("unused")
    public MessageUpdateTile() {}

    public MessageUpdateTile(BlockPos pos, FriendlyByteBuf payload) {
        this.pos = pos;
        this.payload = payload;
        if (getPayloadSize() > MAX_PAYLOAD_SIZE) {
            throw new IllegalStateException("Can't write out " + getPayloadSize() + " bytes!");
        }
    }

    public int getPayloadSize() {
        return payload == null ? 0 : payload.readableBytes();
    }

    public MessageUpdateTile(FriendlyByteBuf buf) {
        pos = buf.readBlockPos();
        int size = buf.readUnsignedMedium();
        if (size > MAX_PAYLOAD_SIZE || size > buf.readableBytes()) {
            throw new DecoderException("Invalid tile update payload size: " + size + " readable=" + buf.readableBytes());
        }
        payload = new FriendlyByteBuf(buf.readBytes(size));
    }

    public static void toBytes(MessageUpdateTile msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        int length = msg.payload.readableBytes();
        if (length > MAX_PAYLOAD_SIZE) {
            throw new IllegalStateException("Tile update payload is too large: " + length);
        }
        buf.writeMedium(length);
        buf.writeBytes(msg.payload, 0, length);
    }

    public static final BiConsumer<MessageUpdateTile, Supplier<NetworkEvent.Context>> HANDLER = (message, ctx) -> {
    	
    	ctx.get().enqueueWork(() -> {
        	try {
                PacketListener netHandler = ctx.get().getNetworkManager().getPacketListener();//TODO
                Level level = null;
                if(netHandler instanceof ServerGamePacketListenerImpl sim) 
                	level = sim.player.level;
                else if(net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient())
                	level = MessageUpdateTileClientHandler.getClientLevel(netHandler);
                if (level == null) {
                    return;
                }
//                BCLog.logger.debug("trying to updata client pipe in "+ message.pos);
                BlockEntity tile = level.getBlockEntity(message.pos);
                if (tile instanceof IPayloadReceiver) {
                	((IPayloadReceiver) tile).receivePayload(ctx.get(), message.payload);
                    return ;
                } else {
//                	level.setBlock(message.pos, Blocks.AIR.defaultBlockState(), 2);
                    BCLog.logger.warn("Dropped message for player " + "null" + " for tile at " + message.pos
                        + " (found " + tile + ")");
                }
                return;
            } catch (IOException | RuntimeException io) {
                BCLog.logger.warn("Dropped invalid BuildCraft tile update packet", io);
            } finally {
  //          	ctx.get().setPacketHandled(true);
                //message.payload.release();
            }
    	  });
    	  ctx.get().setPacketHandled(true);

    	
    };
}
