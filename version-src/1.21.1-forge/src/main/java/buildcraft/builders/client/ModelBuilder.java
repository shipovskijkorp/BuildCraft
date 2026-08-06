package buildcraft.builders.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import buildcraft.api.core.BCLog;
import buildcraft.builders.BCBuildersBlocks;
import buildcraft.lib.client.model.MutableQuad;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.ModelEvent.RegisterAdditional;
import net.minecraftforge.client.model.data.ModelData;

public enum ModelBuilder implements BakedModel{
	
	INSTANCE;
	
	public static final ResourceLocation builder = ResourceLocation.fromNamespaceAndPath("buildcraftbuilders", "builder");
	
	public static final ResourceLocation empty = ResourceLocation.fromNamespaceAndPath("buildcraftbuilders", "block/builder/slot_empty");
	public static final ResourceLocation blueprint = ResourceLocation.fromNamespaceAndPath("buildcraftbuilders", "block/builder/slot_blueprint");
	public static final ResourceLocation template = ResourceLocation.fromNamespaceAndPath("buildcraftbuilders", "block/builder/slot_template");
	
	public static final List<ModelResourceLocation> stateDefinetion = BCBuildersBlocks.BUILDER.get().getStateDefinition().getPossibleStates().stream().map(BlockModelShaper::stateToModelLocation).toList();
		/*{
			new ModelResourceLocation("buildcraftbuilders:builder#facing=south,snapshot_type=template"),
			new ModelResourceLocation("buildcraftbuilders:builder#facing=west,snapshot_type=template"),
			new ModelResourceLocation("buildcraftbuilders:builder#facing=south,snapshot_type=blueprint"),
			new ModelResourceLocation("buildcraftbuilders:builder#facing=west,snapshot_type=none"),
			new ModelResourceLocation("buildcraftbuilders:builder#facing=east,snapshot_type=blueprint"),
			new ModelResourceLocation("buildcraftbuilders:builder#facing=north,snapshot_type=blueprint"),
			new ModelResourceLocation("buildcraftbuilders:builder#facing=north,snapshot_type=template"),
			new ModelResourceLocation("buildcraftbuilders:builder#facing=north,snapshot_type=none"),
			new ModelResourceLocation("buildcraftbuilders:builder#facing=east,snapshot_type=none"),
			new ModelResourceLocation("buildcraftbuilders:builder#facing=east,snapshot_type=template"),
			new ModelResourceLocation("buildcraftbuilders:builder#facing=south,snapshot_type=none"),
			new ModelResourceLocation("buildcraftbuilders:builder#facing=west,snapshot_type=blueprint")
		};;*/
			
    protected static final EnumMap<Direction, List<BakedQuad>> main = new EnumMap<>(Direction.class);
    private static TextureAtlasSprite particleIcon;
	
    public static void init(BakedModel mainModel) {
        particleIcon = mainModel.getParticleIcon();
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
		RandomSource s = RandomSource.create();
		for(Direction  d : Direction.values()) 
			main.put(d, new ArrayList<>(mainModel.getQuads(BCBuildersBlocks.BUILDER.get().defaultBlockState(), d, s, ModelData.EMPTY, null)));
		try {
			resourceManager.openAsReader(ResourceLocation.fromNamespaceAndPath(builder.getNamespace(), "models/" + builder.getPath() + ".json"));
		}
		catch(IOException e) {
			BCLog.logger.error("Cannot load full model of buildcraftbuilders:block/builder "+e.getMessage());
		}
	}
	
	public static void initPart(BakedModel mainModel) {
		RandomSource s = RandomSource.create();
		main.put(Direction.NORTH, mainModel.getQuads(BCBuildersBlocks.BUILDER.get().defaultBlockState(), Direction.NORTH, s, ModelData.EMPTY, null));
		mainModel.getQuads(BCBuildersBlocks.BUILDER.get().defaultBlockState(), Direction.NORTH, s, ModelData.EMPTY, null).stream()
			.map(MutableQuad::creatByBlock)
			.peek((p) -> main.get(Direction.NORTH).add(p.toBakedBlock()))
			.peek((p) -> main.get(Direction.EAST).add(p.rotateZ_90(1).toBakedBlock()))
			.peek((p) -> main.get(Direction.SOUTH).add(p.rotateZ_90(1).toBakedBlock()))
			.forEach((p) -> main.get(Direction.WEST).add(p.rotateZ_90(1).toBakedBlock()));
			
	}
	
    public static void onModelBakePre(RegisterAdditional event) {
    	event.register(new ModelResourceLocation(empty, "standalone"));
    	
    }
	
    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
        if (side == null) {
            return List.of();
        }
        return main.getOrDefault(side, List.of());
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

	@Override
	public boolean isCustomRenderer() {
		return false;
	}

    @Override
    public TextureAtlasSprite getParticleIcon() {
        if (particleIcon != null) {
            return particleIcon;
        }
        return Minecraft.getInstance().getModelManager().getMissingModel().getParticleIcon();
    }

	@Override
	public ItemTransforms getTransforms() {
		return ItemTransforms.NO_TRANSFORMS;
	}

	@Override
	public ItemOverrides getOverrides() {
		return ItemOverrides.EMPTY;
	}

}
