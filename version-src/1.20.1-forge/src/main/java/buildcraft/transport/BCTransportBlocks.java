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

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

public class BCTransportBlocks {
    
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, BCTransport.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BET = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, BCTransport.MODID);

    public static final RegistryObject<BlockFilteredBuffer> filterBuffer = BLOCKS.register("filtered_buffer", BlockFilteredBuffer::new);
    public static final RegistryObject<BlockPipeHolder> pipeHolder = BLOCKS.register("pipe_holder", BlockPipeHolder::new);
    public static final RegistryObject<BlockEntityType<TileFilteredBuffer>> FILTERREDBUFFER_BE = BET.register("entity_filtered_buffer",
    		() -> BlockEntityType.Builder.of(TileFilteredBuffer::new, filterBuffer.get()).build(null));
    public static final RegistryObject<BlockEntityType<TilePipeHolder>> PIPE_HOLDER_BE = BET.register("entity_pipe_holder",
    		() -> BlockEntityType.Builder.of(TilePipeHolder::new, pipeHolder.get()).build(null));
    public static final RegistryObject<BlockItem> FILTERED_BUFFER_ITEM = BCTransportItems.ITEMS.register("filtered_buffer", () -> new BlockItem(filterBuffer.get(), new Item.Properties()));

    public static List<ItemStack> getCreativeTabItems() {
        return List.of(FILTERED_BUFFER_ITEM.get().getDefaultInstance());
    }

    public static void registry(IEventBus b) {
    	BLOCKS.register(b);
    	BET.register(b);
    }
}
