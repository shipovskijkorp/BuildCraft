/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport;

import buildcraft.transport.block.BlockFilteredBuffer;
import buildcraft.transport.block.BlockPipeHolder;
import buildcraft.transport.tile.TileFilteredBuffer;
import buildcraft.transport.tile.TilePipeHolder;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

public class BCTransportBlocks {
    
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, BCTransport.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BET = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BCTransport.MODID);

    public static final DeferredHolder<Block, BlockFilteredBuffer> filterBuffer = BLOCKS.register("filtered_buffer", BlockFilteredBuffer::new);
    public static final DeferredHolder<Block, BlockPipeHolder> pipeHolder = BLOCKS.register("pipe_holder", BlockPipeHolder::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileFilteredBuffer>> FILTERREDBUFFER_BE = BET.register("entity_filtered_buffer",
    		() -> BlockEntityType.Builder.of(TileFilteredBuffer::new, filterBuffer.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TilePipeHolder>> PIPE_HOLDER_BE = BET.register("entity_pipe_holder",
    		() -> BlockEntityType.Builder.of(TilePipeHolder::new, pipeHolder.get()).build(null));
    public static final DeferredHolder<Item, BlockItem> FILTERED_BUFFER_ITEM = BCTransportItems.ITEMS.register("filtered_buffer", () -> new BlockItem(filterBuffer.get(), new Item.Properties()));

    public static List<ItemStack> getCreativeTabItems() {
        return List.of(FILTERED_BUFFER_ITEM.get().getDefaultInstance());
    }

    public static void registry(IEventBus b) {
    	BLOCKS.register(b);
    	BET.register(b);
    }
}
