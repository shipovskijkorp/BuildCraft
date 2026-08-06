package buildcraft.builders;

import buildcraft.builders.block.BlockArchitectTable;
import buildcraft.builders.block.BlockBuilder;
import buildcraft.builders.block.BlockConstructionMarker;
import buildcraft.builders.block.BlockElectronicLibrary;
import buildcraft.builders.block.BlockFiller;
import buildcraft.builders.block.BlockFrame;
import buildcraft.builders.block.BlockQuarry;
import buildcraft.builders.block.BlockQuarryDrillCollision;
import buildcraft.builders.block.BlockReplacer;
import buildcraft.builders.tile.TileArchitectTable;
import buildcraft.builders.tile.TileBuilder;
import buildcraft.builders.tile.TileConstructionMarker;
import buildcraft.builders.tile.TileElectronicLibrary;
import buildcraft.builders.tile.TileFiller;
import buildcraft.builders.tile.TileQuarry;
import buildcraft.builders.tile.TileQuarryDrillCollision;
import buildcraft.builders.tile.TileReplacer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BCBuildersBlocks {

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, BCBuilders.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITYS = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, BCBuilders.MODID);

    public static final RegistryObject<BlockFiller> FILLER = BLOCKS.register("filler", BlockFiller::new);
    public static final RegistryObject<BlockBuilder> BUILDER = BLOCKS.register("builder", BlockBuilder::new);
    public static final RegistryObject<BlockArchitectTable> ARCHITECT = BLOCKS.register("architect", BlockArchitectTable::new);
    public static final RegistryObject<BlockElectronicLibrary> LIBRARY = BLOCKS.register("library", BlockElectronicLibrary::new);
    public static final RegistryObject<BlockReplacer> REPLACER = BLOCKS.register("replacer", BlockReplacer::new);
    public static final RegistryObject<BlockConstructionMarker> CONSTRUCTION_MARKER = BLOCKS.register("marker_construction", BlockConstructionMarker::new);
    
    public static final RegistryObject<BlockFrame> FRAME = BLOCKS.register("frame", BlockFrame::new);
    public static final RegistryObject<BlockQuarry> QUARRY = BLOCKS.register("quarry", BlockQuarry::new);
    public static final RegistryObject<BlockQuarryDrillCollision> QUARRY_DRILL_COLLISION = BLOCKS.register("quarry_drill_collision", BlockQuarryDrillCollision::new);

    
    public static final RegistryObject<BlockEntityType<TileFiller>> FILLER_TILE_BC8 = BLOCK_ENTITYS.register("entity_filler", 
    		() -> BlockEntityType.Builder.of(TileFiller::new, FILLER.get()).build(null));
    
    public static final RegistryObject<BlockEntityType<TileBuilder>> BUILDER_TILE_BC8 = BLOCK_ENTITYS.register("entity_builder", 
    		() -> BlockEntityType.Builder.of(TileBuilder::new, BUILDER.get()).build(null));
    
    public static final RegistryObject<BlockEntityType<TileArchitectTable>> ARCHITECT_TILE_BC8 = BLOCK_ENTITYS.register("entity_architect", 
    		() -> BlockEntityType.Builder.of(TileArchitectTable::new, ARCHITECT.get()).build(null));
    
    public static final RegistryObject<BlockEntityType<TileElectronicLibrary>> LIBRARY_TILE_BC8 = BLOCK_ENTITYS.register("entity_library", 
    		() -> BlockEntityType.Builder.of(TileElectronicLibrary::new, LIBRARY.get()).build(null));
    
    public static final RegistryObject<BlockEntityType<TileReplacer>> REPLACER_TILE_BC8 = BLOCK_ENTITYS.register("entity_replacer", 
    		() -> BlockEntityType.Builder.of(TileReplacer::new, REPLACER.get()).build(null));
    
    public static final RegistryObject<BlockEntityType<TileConstructionMarker>> CONSTRUCTION_MARKER_TILE_BC8 = BLOCK_ENTITYS.register("entity_marker_construction", 
            () -> BlockEntityType.Builder.of(TileConstructionMarker::new, CONSTRUCTION_MARKER.get()).build(null));
    
    
    public static final RegistryObject<BlockEntityType<TileQuarry>> QUARRY_TILE_BC8 = BLOCK_ENTITYS.register("entity_quarry", 
    		() -> BlockEntityType.Builder.of(TileQuarry::new, QUARRY.get()).build(null));

    public static final RegistryObject<BlockEntityType<TileQuarryDrillCollision>> QUARRY_DRILL_COLLISION_TILE_BC8 = BLOCK_ENTITYS.register("entity_quarry_drill_collision",
            () -> BlockEntityType.Builder.of(TileQuarryDrillCollision::new, QUARRY_DRILL_COLLISION.get()).build(null));
    
    public static void registry(IEventBus b) {
    	BLOCKS.register(b);
    	BLOCK_ENTITYS.register(b);
    }
}
