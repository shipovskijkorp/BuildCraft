package buildcraft.robotics.item;

import buildcraft.lib.misc.ItemStackUtil;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import buildcraft.api.robots.DockingStation;
import buildcraft.api.robots.EntityRobotBase;
import buildcraft.robotics.BCRoboticsBoards;
import buildcraft.robotics.BCRoboticsBoards.BoardEntry;
import buildcraft.robotics.BCRoboticsItems;

import buildcraft.api.events.RobotEvent;
import buildcraft.robotics.entity.EntityRobot;
import buildcraft.robotics.plug.RobotStationPluggable;
import buildcraft.transport.tile.TilePipeHolder;
import buildcraft.transport.block.BlockPipeHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import buildcraft.lib.item.ICreativeTabItemProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class ItemRobot extends Item implements ICreativeTabItemProvider {
    public ItemRobot(Properties properties) {
        super(properties);
    }

    public static ItemStack createRobotStack(BoardEntry board, int energy) {
        ItemStack stack = new ItemStack(BCRoboticsItems.ROBOT.get());
        CompoundTag boardTag = new CompoundTag();
        board.nbt().createBoard(boardTag);
        CompoundTag tag = new CompoundTag();
        tag.put("board", boardTag);
        tag.putInt("energy", Math.max(0, Math.min(EntityRobotBase.MAX_ENERGY, energy)));
        tag.putString("robot_key", board.key());
        ItemStackUtil.setCustomData(stack, tag);
        return stack;
    }

    public static int getEnergy(ItemStack stack) {
        return ItemStackUtil.getCustomData(stack).getInt("energy");
    }

    @Override
    public void addCreativeTabItems(Consumer<ItemStack> output) {
        BCRoboticsBoards.init();
        output.accept(createRobotStack(BCRoboticsBoards.EMPTY, 0));
        for (BoardEntry board : BCRoboticsBoards.robotEntries()) {
            output.accept(createRobotStack(board, 0));
            output.accept(createRobotStack(board, EntityRobotBase.MAX_ENERGY));
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        BoardEntry board = BCRoboticsBoards.getRobotBoard(stack);
        if (board == BCRoboticsBoards.EMPTY) {
            return Component.translatable("item.buildcraftrobotics.robot.empty");
        }
        return Component.translatable("item.buildcraftrobotics.robot." + board.key());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BoardEntry board = BCRoboticsBoards.getRobotBoard(stack);
        if (board != BCRoboticsBoards.EMPTY) {
            String legacyKey = board.legacyLangKey();
            tooltip.add(Component.translatable("buildcraft." + legacyKey).withStyle(ChatFormatting.BOLD));
            tooltip.add(Component.translatable("buildcraft." + legacyKey + ".desc").withStyle(ChatFormatting.GRAY));
            if (board.isInDev()) {
                tooltip.add(Component.translatable("tooltip.buildcraftrobotics.in_dev").withStyle(ChatFormatting.RED));
            }
            tooltip.add(Component.translatable("tooltip.buildcraftrobotics.robot.energy",
                    EntityRobot.formatRobotEnergy(getEnergy(stack)), EntityRobot.formatRobotEnergy(EntityRobotBase.MAX_ENERGY))
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    public EntityRobot createRobot(ItemStack stack, Level level) {
        BoardEntry board = BCRoboticsBoards.getRobotBoard(stack);
        if (board == BCRoboticsBoards.EMPTY) {
            return null;
        }
        EntityRobot robot = new EntityRobot(level, board);
        robot.setEnergy(getEnergy(stack));
        return robot;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        BlockEntity blockEntity = level.getBlockEntity(context.getClickedPos());
        if (!(blockEntity instanceof TilePipeHolder tile)) {
            return InteractionResult.PASS;
        }

        Direction side = BlockPipeHolder.rayTracePluggableSide(level, context.getClickedPos(), player);
        if (side == null) {
            return InteractionResult.PASS;
        }
        if (!(tile.getPluggable(side) instanceof RobotStationPluggable station)) {
            return InteractionResult.PASS;
        }
        return placeOnStation(context.getItemInHand(), player, level, tile, side, station);
    }

    public static InteractionResult placeOnStation(ItemStack currentItem, Player player, Level level, TilePipeHolder tile,
                                                   Direction side, RobotStationPluggable pluggable) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        DockingStation station = pluggable.getStation();
        if (station == null || station.isTaken()) {
            return InteractionResult.SUCCESS;
        }
        BoardEntry board = BCRoboticsBoards.getRobotBoard(currentItem);
        if (board == BCRoboticsBoards.EMPTY) {
            return InteractionResult.SUCCESS;
        }
        EntityRobot robot = ((ItemRobot) currentItem.getItem()).createRobot(currentItem, level);
        if (robot == null || robot.getRegistry() == null) {
            return InteractionResult.SUCCESS;
        }
        robot.setOwner(player.getGameProfile());
        RobotEvent.Place robotEvent = new RobotEvent.Place(robot, player);
        NeoForge.EVENT_BUS.post(robotEvent);
        if (robotEvent.isCanceled()) {
            return InteractionResult.SUCCESS;
        }

        robot.setUniqueRobotId(robot.getRegistry().getNextRobotId());
        BlockPos pos = tile.getPipePos();
        robot.setPos(pos.getX() + 0.5D + side.getStepX() * 0.5D,
                pos.getY() + 0.5D + side.getStepY() * 0.5D,
                pos.getZ() + 0.5D + side.getStepZ() * 0.5D);
        if (station.takeAsMain(robot)) {
            robot.dock(robot.getLinkedStation());
            level.addFreshEntity(robot);
            tile.scheduleRenderUpdate();
            if (!player.isCreative()) {
                currentItem.shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }

}
