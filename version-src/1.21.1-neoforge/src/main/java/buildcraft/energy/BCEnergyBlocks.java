package buildcraft.energy;

import java.util.List;

import buildcraft.api.enums.EnumEngineType;
import buildcraft.core.BCCoreBlocks;
import buildcraft.core.BCCoreItems;
import buildcraft.energy.tile.TileEngineIron_BC8;
import buildcraft.energy.tile.TileEngineStone_BC8;
import buildcraft.energy.tile.TileSpringOil;
import buildcraft.lib.item.MultiBlockItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BCEnergyBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, BCEnergy.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITYS =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BCEnergy.MODID);

    public static final DeferredHolder<Item, MultiBlockItem<EnumEngineType>> ENGINE_STONE_ITEM = BCEnergy.ITEMS.register(
        "engine_stone",
        () -> new MultiBlockItem<>(
            BCCoreBlocks.ENGINE_BC8.get(), new Item.Properties(), EnumEngineType.STONE, BCCoreItems.ENGINE_ITEM_MAP
        )
    );
    public static final DeferredHolder<Item, MultiBlockItem<EnumEngineType>> ENGINE_IRON_ITEM = BCEnergy.ITEMS.register(
        "engine_iron",
        () -> new MultiBlockItem<>(
            BCCoreBlocks.ENGINE_BC8.get(), new Item.Properties(), EnumEngineType.IRON, BCCoreItems.ENGINE_ITEM_MAP
        )
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEngineStone_BC8>> ENGINE_STONE_TILE_BC8 =
        BLOCK_ENTITYS.register(
            "entity_stone_engine",
            () -> BlockEntityType.Builder.of(TileEngineStone_BC8::new, BCCoreBlocks.ENGINE_BC8.get()).build(null)
        );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEngineIron_BC8>> ENGINE_IRON_TILE_BC8 =
        BLOCK_ENTITYS.register(
            "entity_iron_engine",
            () -> BlockEntityType.Builder.of(TileEngineIron_BC8::new, BCCoreBlocks.ENGINE_BC8.get()).build(null)
        );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileSpringOil>> TILE_SPRING =
        BLOCK_ENTITYS.register(
            "entity_spring",
            () -> BlockEntityType.Builder.of(TileSpringOil::new, BCCoreBlocks.SPRING.get()).build(null)
        );

    static void init(IEventBus bus) {
        BLOCK_ENTITYS.register(bus);
        BLOCKS.register(bus);
    }

    public static List<ItemStack> getCreativeTabItems() {
        return List.of(ENGINE_STONE_ITEM.get().getDefaultInstance(), ENGINE_IRON_ITEM.get().getDefaultInstance());
    }
}
