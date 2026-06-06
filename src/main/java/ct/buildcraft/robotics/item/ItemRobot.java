package ct.buildcraft.robotics.item;

import java.util.List;

import javax.annotation.Nullable;

import ct.buildcraft.api.robots.EntityRobotBase;
import ct.buildcraft.robotics.BCRobotics;
import ct.buildcraft.robotics.BCRoboticsBoards;
import ct.buildcraft.robotics.BCRoboticsBoards.BoardEntry;
import ct.buildcraft.robotics.BCRoboticsItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class ItemRobot extends Item {
    public ItemRobot(Properties properties) {
        super(properties);
    }

    public static ItemStack createRobotStack(BoardEntry board, int energy) {
        ItemStack stack = new ItemStack(BCRoboticsItems.ROBOT.get());
        CompoundTag boardTag = new CompoundTag();
        board.nbt().createBoard(boardTag);
        CompoundTag tag = stack.getOrCreateTag();
        tag.put("board", boardTag);
        tag.putInt("energy", Math.max(0, Math.min(EntityRobotBase.MAX_ENERGY, energy)));
        tag.putString("robot_key", board.key());
        return stack;
    }

    public static int getEnergy(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : tag.getInt("energy");
    }

    @Override
    public void fillItemCategory(CreativeModeTab tab, NonNullList<ItemStack> list) {
        if (!allowedIn(tab)) {
            return;
        }
        BCRoboticsBoards.init();
        list.add(createRobotStack(BCRoboticsBoards.EMPTY, 0));
        for (BoardEntry board : BCRoboticsBoards.robotEntries()) {
            list.add(createRobotStack(board, 0));
            list.add(createRobotStack(board, EntityRobotBase.MAX_ENERGY));
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        BoardEntry board = BCRoboticsBoards.getRobotBoard(stack);
        if (board == BCRoboticsBoards.EMPTY) {
            return Component.translatable("item.buildcraftrobotics.robot.empty");
        }
        Component robotName = Component.translatable("item.buildcraftrobotics.robot." + board.key());
        if (getEnergy(stack) >= EntityRobotBase.MAX_ENERGY) {
            return Component.translatable("item.buildcraftrobotics.robot.charged", robotName);
        }
        return robotName;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        BoardEntry board = BCRoboticsBoards.getRobotBoard(stack);
        if (board != BCRoboticsBoards.EMPTY) {
            tooltip.add(Component.translatable("tooltip.buildcraftrobotics.robot.board", Component.translatable("item.buildcraftrobotics.redstone_board." + board.key()))
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.buildcraftrobotics.robot.energy", getEnergy(stack), EntityRobotBase.MAX_ENERGY)
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
