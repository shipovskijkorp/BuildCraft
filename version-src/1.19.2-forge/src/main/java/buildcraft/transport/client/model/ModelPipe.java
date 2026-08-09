/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.client.model;

import java.util.HashMap;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import buildcraft.api.core.BCLog;
import buildcraft.lib.misc.SpriteUtil;
import buildcraft.transport.block.BlockPipeHolder;
import buildcraft.transport.client.model.PipeModelCacheAll.PipeAllCutoutKey;
import buildcraft.transport.client.model.PipeModelCacheAll.PipeAllTranslucentKey;
import buildcraft.transport.client.model.PipeModelCacheBase.PipeBaseCutoutKey;
import buildcraft.transport.client.model.key.PipeModelKey;
import buildcraft.transport.pipe.Pipe;
import buildcraft.transport.tile.TilePipeHolder;
import com.google.common.collect.ImmutableList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

public enum ModelPipe implements IDynamicBakedModel {
	INSTANCE;

	public static final ModelProperty<TilePipeHolder> PipeTypeModelKey = new ModelProperty<TilePipeHolder>();
/*	protected static final Direction[][] LiteraDirection = {
			{ Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST },
			{ Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP },
			{ Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH } };
	protected static final EnumMap<Direction, BakedQuad[]> facesCacheBase = new EnumMap<>(Direction.class);// {up, left,
																											// down,
																											// right,
																											// center,centerHigh}
	protected static final EnumMap<Direction, BakedQuad[]> facesCacheCombine = new EnumMap<>(Direction.class);*/

	protected static final HashMap<ResourceLocation, TextureAtlasSprite> particleIcon = new HashMap<>();

	public static void clearTextureCache() {
		particleIcon.clear();
	}
	@Override
	public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
			@NotNull RandomSource rand, @NotNull ModelData data, @Nullable RenderType renderType) {
		TilePipeHolder tile = data.get(PipeTypeModelKey);
        if (tile == null || tile.getPipe() == Pipe.EMPTY) {
            if (renderType == RenderType.translucent()) {
                return ImmutableList.of();
            }
            return PipeModelCacheBase.cacheCutout.bake(new PipeBaseCutoutKey(PipeModelKey.DEFAULT_KEY));
        }

        if (renderType == RenderType.translucent()) {
            PipeAllTranslucentKey realKey = new PipeAllTranslucentKey(tile);
            return PipeModelCacheAll.cacheTranslucent.bake(realKey);
        } else {
            PipeAllCutoutKey realKey = new PipeAllCutoutKey(tile);
            return PipeModelCacheAll.cacheCutout.bake(realKey);
        }

	}
	
	
	@Override
	public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand,
			@NotNull ModelData data) {
		return ChunkRenderTypeSet.of(List.of(RenderType.cutout(), RenderType.translucent()));
	}

	@Override
	public boolean useAmbientOcclusion() {
		return true;
	}

	@Override
	public boolean isGui3d() {
		return false;
	}

	@Override
	public boolean isCustomRenderer() {
		return false;
	}

	@Override
	public boolean usesBlockLight() {
		return true;
	}

	@Override
	public TextureAtlasSprite getParticleIcon() {
		return SpriteUtil.missingSprite();
	}
	
	/**
	 * @see BlockPipeHolder#addDestroyEffects(BlockState, Level, BlockPos, ParticleEngine)
	 * */
	@Override
	public TextureAtlasSprite getParticleIcon(ModelData data) {
		if(data == ModelData.EMPTY)
			return SpriteUtil.missingSprite();
		ResourceLocation identifier = data.get(PipeTypeModelKey).getPipe().definition.textures[0];//TODO find correct tex
		return particleIcon.computeIfAbsent(identifier,
				(a) -> {
					TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(a);
					if(sprite == SpriteUtil.missingSprite())
						BCLog.logger.error("ModelPipe:empty spite for "+a);
					return sprite;
				});
	}

	@Override
	public ItemOverrides getOverrides() {
		return ItemOverrides.EMPTY;
	}

}
