/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.item;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import buildcraft.api.core.IAreaProvider;
import buildcraft.api.core.IBox;
import buildcraft.api.core.IPathProvider;
import buildcraft.api.core.IZone;
import buildcraft.api.items.IMapLocation;
import buildcraft.lib.misc.ItemStackUtil;
import buildcraft.lib.misc.NBTUtilBC;
import buildcraft.lib.misc.StackUtil;
import buildcraft.lib.misc.StringUtilBC;
import buildcraft.lib.misc.data.Box;
import buildcraft.robotics.zone.ZonePlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ItemMapLocation extends Item implements IMapLocation {
    private static final String[] STORAGE_TAGS = "kind,Damage,x,y,z,side,xMin,xMax,yMin,yMax,zMin,zMax,path,chunkMapping,name".split(",");

    public ItemMapLocation(Item.Properties prop) {
        super(prop);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> strings, TooltipFlag flag) {
        stack = StackUtil.asNonNull(stack);
        CompoundTag cpt = ItemStackUtil.getCustomData(stack);
        if (cpt.contains("name")) {
            String name = cpt.getString("name");
            if (name.length() > 0) {
                strings.add(Component.literal(name));
            }
        }

        MapLocationType type = MapLocationType.getFromStack(stack);
        switch (type) {
            case SPOT: {
                if (cpt.contains("x") && cpt.contains("y") && cpt.contains("z") && cpt.contains("side")) {
                    int x = cpt.getInt("x");
                    int y = cpt.getInt("y");
                    int z = cpt.getInt("z");
                    Direction side = getPointFace(stack);
                    strings.add(Component.literal("{" + x + ", " + y + ", " + z + ", " + side + "}"));
                }
                break;
            }
            case AREA: {
                if (cpt.contains("xMin") && cpt.contains("yMin") && cpt.contains("zMin") && cpt.contains("xMax")
                    && cpt.contains("yMax") && cpt.contains("zMax")) {
                    int x = cpt.getInt("xMin");
                    int y = cpt.getInt("yMin");
                    int z = cpt.getInt("zMin");
                    int xLength = cpt.getInt("xMax") - x + 1;
                    int yLength = cpt.getInt("yMax") - y + 1;
                    int zLength = cpt.getInt("zMax") - z + 1;

                    strings.add(Component.literal(
                        "{" + x + ", " + y + ", " + z + "} + {" + xLength + " x " + yLength + " x " + zLength + "}"));//TODO
                }
                break;
            }
            case PATH:
            case PATH_REPEATING: {
                if (cpt.contains("path")) {
                    ListTag pathNBT = (ListTag) cpt.get("path");
                    if (pathNBT.size() > 0) {
                        BlockPos first = NBTUtilBC.readBlockPos(pathNBT.get(0));
                        if (first != null) {
                            strings.add(Component.literal("{"+
                                StringUtilBC.blockPosToString(first) + "}, (+" + (pathNBT.size() - 1) + " elements)"));
                        }
                    }
                }
                break;
            }
            default: {
                break;
            }
        }
        if (type != MapLocationType.CLEAN) {
            strings.add(Component.translatable("buildcraft.item.nonclean.usage"));
        }
    }


	@Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (world.isClientSide) {
            return new InteractionResultHolder<>(InteractionResult.PASS, stack);
        }
        if (player.isShiftKeyDown()) {
            return clearMarkerData(stack);
        }
        return new InteractionResultHolder<>(InteractionResult.PASS, stack);
    }

    private static InteractionResultHolder<ItemStack> clearMarkerData(@Nonnull ItemStack stack) {
        if (MapLocationType.getFromStack(stack) == MapLocationType.CLEAN) {
            return new InteractionResultHolder<>(InteractionResult.PASS, stack);
        }
        CompoundTag nbt = ItemStackUtil.getCustomData(stack);
        for (String key : STORAGE_TAGS) {
            nbt.remove(key);
        }
        ItemStackUtil.setCustomData(stack, nbt);
        MapLocationType.CLEAN.setToStack(stack);
        return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Level world = ctx.getLevel();
        if (world.isClientSide) {
            return InteractionResult.PASS;
        }

        Player player = ctx.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            return clearMarkerData(stack).getResult();
        }

        return writeLocationToMap(ctx, StackUtil.asNonNull(stack));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();
        if (world.isClientSide) {
            return InteractionResult.PASS;
        }

        Player player = ctx.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            return clearMarkerData(StackUtil.asNonNull(ctx.getItemInHand())).getResult();
        }

        return writeLocationToMap(ctx, StackUtil.asNonNull(ctx.getItemInHand()));
    }

    private static InteractionResult writeLocationToMap(UseOnContext ctx, ItemStack stack) {
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();

        if (MapLocationType.getFromStack(stack) != MapLocationType.CLEAN) {
            return InteractionResult.FAIL;
        }

        ItemStack modified = stack;
        boolean splitFromStack = player != null && stack.getCount() > 1 && !player.getAbilities().instabuild;
        if (splitFromStack) {
            modified = stack.copy();
            modified.setCount(1);
            stack.shrink(1);
        }

        BlockEntity tile = world.getBlockEntity(pos);
        MapLocationType type;
        CompoundTag cpt = ItemStackUtil.getCustomData(modified);

        if (tile instanceof IPathProvider pathTile) {
            List<BlockPos> path = pathTile.getPath();
            type = path.size() > 1 && path.get(0).equals(path.get(path.size() - 1))
                ? MapLocationType.PATH_REPEATING
                : MapLocationType.PATH;

            ListTag pathNBT = new ListTag();
            for (BlockPos posInPath : path) {
                pathNBT.add(NbtUtils.writeBlockPos(posInPath));
            }
            cpt.put("path", pathNBT);
        } else if (tile instanceof IAreaProvider areaTile) {
            type = MapLocationType.AREA;
            cpt.putInt("xMin", areaTile.min().getX());
            cpt.putInt("yMin", areaTile.min().getY());
            cpt.putInt("zMin", areaTile.min().getZ());
            cpt.putInt("xMax", areaTile.max().getX());
            cpt.putInt("yMax", areaTile.max().getY());
            cpt.putInt("zMax", areaTile.max().getZ());
        } else {
            type = MapLocationType.SPOT;
            cpt.putByte("side", (byte) ctx.getClickedFace().get3DDataValue());
            cpt.putInt("x", pos.getX());
            cpt.putInt("y", pos.getY());
            cpt.putInt("z", pos.getZ());
        }

        ItemStackUtil.setCustomData(modified, cpt);
        type.setToStack(modified);

        if (splitFromStack && !modified.isEmpty()) {
            if (!player.getInventory().add(modified)) {
                player.drop(modified, false);
            }
        }

        return InteractionResult.SUCCESS;
    }

    public static IBox getAreaBox(@Nonnull ItemStack item) {
        CompoundTag cpt = ItemStackUtil.getCustomData(item);
        int xMin = cpt.getInt("xMin");
        int yMin = cpt.getInt("yMin");
        int zMin = cpt.getInt("zMin");
        BlockPos min = new BlockPos(xMin, yMin, zMin);

        int xMax = cpt.getInt("xMax");
        int yMax = cpt.getInt("yMax");
        int zMax = cpt.getInt("zMax");
        BlockPos max = new BlockPos(xMax, yMax, zMax);

        return new Box(min, max);
    }

    public static IBox getPointBox(@Nonnull ItemStack item) {
        CompoundTag cpt = ItemStackUtil.getCustomData(item);
        MapLocationType type = MapLocationType.getFromStack(item);

        switch (type) {
            case SPOT: {
                int x = cpt.getInt("x");
                int y = cpt.getInt("y");
                int z = cpt.getInt("z");

                BlockPos pos = new BlockPos(x, y, z);

                return new Box(pos, pos);
            }
            default: {
                return null;
            }
        }
    }

    public static Direction getPointFace(@Nonnull ItemStack stack) {
        CompoundTag cpt = ItemStackUtil.getCustomData(stack);
        int side = cpt.getByte("side");
        Direction[] values = Direction.values();
        if (side < 0 || side >= values.length) {
            return Direction.UP;
        }
        return values[side];
    }

    @Override
    public IBox getBox(@Nonnull ItemStack item) {
        MapLocationType type = MapLocationType.getFromStack(item);

        switch (type) {
            case AREA: {
                return getAreaBox(item);
            }
            case SPOT: {
                return getPointBox(item);
            }
            default: {
                return null;
            }
        }
    }

    @Override
    public Direction getPointSide(@Nonnull ItemStack item) {
        CompoundTag cpt = ItemStackUtil.getCustomData(item);
        MapLocationType type = MapLocationType.getFromStack(item);

        if (type == MapLocationType.SPOT) {
            return getPointFace(item);
        } else {
            return null;
        }
    }

    @Override
    public BlockPos getPoint(@Nonnull ItemStack item) {
        CompoundTag cpt = ItemStackUtil.getCustomData(item);
        MapLocationType type = MapLocationType.getFromStack(item);

        if (type == MapLocationType.SPOT) {
            return new BlockPos(cpt.getInt("x"), cpt.getInt("y"), cpt.getInt("z"));
        } else {
            return null;
        }
    }

    @Override
    public IZone getZone(@Nonnull ItemStack item) {
        CompoundTag cpt = ItemStackUtil.getCustomData(item);
        MapLocationType type = MapLocationType.getFromStack(item);
        switch (type) {
            case ZONE: {
                ZonePlan plan = new ZonePlan();
                plan.readFromNBT(cpt);
                return plan;
            }
            case AREA: {
                return getBox(item);
            }
            case SPOT:
            case PATH:
            case PATH_REPEATING: {
                return getPointBox(item);
            }
            default: {
                return null;
            }
        }
    }

    @Override
    public List<BlockPos> getPath(@Nonnull ItemStack item) {
        CompoundTag cpt = ItemStackUtil.getCustomData(item);
        MapLocationType type = MapLocationType.getFromStack(item);
        switch (type) {
            case PATH:
            case PATH_REPEATING: {
                List<BlockPos> indexList = new ArrayList<>();
                if (!(cpt.get("path") instanceof ListTag pathNBT)) {
                    return indexList;
                }
                for (int i = 0; i < pathNBT.size(); i++) {
                    BlockPos pos = NBTUtilBC.readBlockPos(pathNBT.get(i));
                    if (pos != null) {
                        indexList.add(pos);
                    }
                }
                return indexList;
            }
            case SPOT: {
                List<BlockPos> indexList = new ArrayList<>();
                indexList.add(new BlockPos(cpt.getInt("x"), cpt.getInt("y"), cpt.getInt("z")));
                return indexList;
            }
            default: {
                return null;
            }
        }
    }

    public static void setZone(@Nonnull ItemStack item, ZonePlan plan) {
        CompoundTag cpt = ItemStackUtil.getCustomData(item);
        plan.writeToNBT(cpt);
        ItemStackUtil.setCustomData(item, cpt);
        MapLocationType.ZONE.setToStack(item);
    }

    @Override
    public String getLabelName(@Nonnull ItemStack item) {
        return ItemStackUtil.getCustomData(item).getString("name");
    }

    @Override
    public boolean setLabelName(@Nonnull ItemStack item, String name) {
        CompoundTag cpt = ItemStackUtil.getCustomData(item);
        cpt.putString("name", name);
        ItemStackUtil.setCustomData(item, cpt);
        return true;
    }
}
