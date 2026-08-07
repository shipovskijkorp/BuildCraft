package buildcraft.robotics;

import buildcraft.robotics.block.BlockRequester;
import buildcraft.robotics.block.BlockZonePlanner;
import buildcraft.robotics.tile.TileRequester;
import buildcraft.robotics.tile.TileZonePlanner;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class BCRoboticsBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, BCRobotics.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITYS = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BCRobotics.MODID);

    public static final DeferredHolder<Block, BlockZonePlanner> ZONE_PLANNER = BLOCKS.register("zone_planner", BlockZonePlanner::new);
    public static final DeferredHolder<Block, BlockRequester> REQUESTER = BLOCKS.register("requester", BlockRequester::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileZonePlanner>> ZONE_PLANNER_TILE = BLOCK_ENTITYS.register(
            "entity_zone_planner",
            () -> BlockEntityType.Builder.of(TileZonePlanner::new, ZONE_PLANNER.get()).build(null)
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileRequester>> REQUESTER_TILE = BLOCK_ENTITYS.register(
            "entity_requester",
            () -> BlockEntityType.Builder.of(TileRequester::new, REQUESTER.get()).build(null)
    );

    private BCRoboticsBlocks() {
    }

    public static void registry(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ENTITYS.register(bus);
    }
}
