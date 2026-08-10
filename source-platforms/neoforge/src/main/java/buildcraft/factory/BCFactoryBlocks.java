package buildcraft.factory;

import buildcraft.factory.block.BlockAutoWorkbenchItems;
import buildcraft.factory.block.BlockChute;
import buildcraft.factory.block.BlockDistiller;
import buildcraft.factory.block.BlockFloodGate;
import buildcraft.factory.block.BlockHeatExchange;
import buildcraft.factory.block.BlockMiningWell;
import buildcraft.factory.block.BlockPump;
import buildcraft.factory.block.BlockTank;
import buildcraft.factory.block.BlockTube;
import buildcraft.factory.block.BlockWaterGel;
import buildcraft.factory.tile.TileAutoWorkbenchItems;
import buildcraft.factory.tile.TileChute;
import buildcraft.factory.tile.TileDistiller_BC8;
import buildcraft.factory.tile.TileFloodGate;
import buildcraft.factory.tile.TileHeatExchange;
import buildcraft.factory.tile.TileMiningWell;
import buildcraft.factory.tile.TilePump;
import buildcraft.factory.tile.TileTank;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BCFactoryBlocks {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITYS =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BCFactory.MODID);
    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(Registries.BLOCK, BCFactory.MODID);

    public static final DeferredHolder<Block, BlockPump> PUMP_BLOCK = BLOCKS.register("pump", BlockPump::new);
    public static final DeferredHolder<Block, BlockTank> TANK_BLOCK = BLOCKS.register("tank", BlockTank::new);
    public static final DeferredHolder<Block, BlockChute> CHUTE_BLOCK = BLOCKS.register("chute", BlockChute::new);
    public static final DeferredHolder<Block, BlockFloodGate> FLOOD_GATE_BLOCK =
        BLOCKS.register("flood_gate", BlockFloodGate::new);
    public static final DeferredHolder<Block, BlockTube> TUBE_BLOCK = BLOCKS.register("tube", BlockTube::new);
    public static final DeferredHolder<Block, BlockMiningWell> MINING_WELL_BLOCK =
        BLOCKS.register("mining_well", BlockMiningWell::new);
    public static final DeferredHolder<Block, BlockDistiller> DISTILLER_BLOCK =
        BLOCKS.register("distiller", BlockDistiller::new);
    public static final DeferredHolder<Block, BlockHeatExchange> HEATEXCHANGE_BLOCK =
        BLOCKS.register("heat_exchange", BlockHeatExchange::new);
    public static final DeferredHolder<Block, BlockWaterGel> WATER_GEL = BLOCKS.register("water_gel", BlockWaterGel::new);
    public static final DeferredHolder<Block, BlockAutoWorkbenchItems> AUTO_BENCH_BLOCK =
        BLOCKS.register("autoworkbench_item", BlockAutoWorkbenchItems::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileTank>> ENTITYBLOCKTANK =
        BLOCK_ENTITYS.register("entity_tank",
            () -> BlockEntityType.Builder.of(TileTank::new, TANK_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TilePump>> ENTITYBLOCKPUMP =
        BLOCK_ENTITYS.register("entity_pump",
            () -> BlockEntityType.Builder.of(TilePump::new, PUMP_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileFloodGate>> ENTITYBLOCKFLOODGATE =
        BLOCK_ENTITYS.register("entity_flood_gate",
            () -> BlockEntityType.Builder.of(TileFloodGate::new, FLOOD_GATE_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileMiningWell>> ENTITYBLOCKMININGWELL =
        BLOCK_ENTITYS.register("entity_mining_well",
            () -> BlockEntityType.Builder.of(TileMiningWell::new, MINING_WELL_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileDistiller_BC8>> ENTITYBLOCKDISTILLER =
        BLOCK_ENTITYS.register("entity_distiller",
            () -> BlockEntityType.Builder.of(TileDistiller_BC8::new, DISTILLER_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileHeatExchange>> ENTITYBLOCKHEATEXCHANGE =
        BLOCK_ENTITYS.register("entity_heat_exchange",
            () -> BlockEntityType.Builder.of(TileHeatExchange::new, HEATEXCHANGE_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileAutoWorkbenchItems>> ENTITYBLOCKAUTOBENCH =
        BLOCK_ENTITYS.register("entity_autoworkbench_item",
            () -> BlockEntityType.Builder.of(TileAutoWorkbenchItems::new, AUTO_BENCH_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileChute>> ENTITYBLOCKCHUTE =
        BLOCK_ENTITYS.register("entity_chute",
            () -> BlockEntityType.Builder.of(TileChute::new, CHUTE_BLOCK.get()).build(null));

    static void registry(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ENTITYS.register(bus);
    }
}
