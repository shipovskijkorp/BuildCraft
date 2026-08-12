package buildcraft.robotics.boards;

import buildcraft.robotics.internal.legacy.boards.RedstoneBoardRobot;
import buildcraft.robotics.internal.legacy.boards.RedstoneBoardRobotNBT;
import buildcraft.lib.internal.core.BlockIndex;
import buildcraft.lib.internal.core.IStackFilter;
import buildcraft.robotics.internal.legacy.robots.AIRobot;
import buildcraft.robotics.internal.legacy.robots.EntityRobotBase;
import buildcraft.robotics.internal.legacy.robots.ResourceIdBlock;
import buildcraft.robotics.BCRoboticsBoards;
import buildcraft.robotics.ai.AIRobotFetchAndEquipItemStack;
import buildcraft.robotics.ai.AIRobotGotoSleep;
import buildcraft.robotics.ai.AIRobotGotoStationAndUnload;
import buildcraft.robotics.ai.AIRobotSearchAndGotoBlock;
import buildcraft.robotics.ai.AIRobotStripesHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Classic BuildCraft Stripes robot: fetches an item and applies the Stripes pipe item-use handlers to a free block. */
public class BoardRobotStripes extends RedstoneBoardRobot {
    private BlockIndex blockFound;

    public BoardRobotStripes(EntityRobotBase robot) {
        super(robot);
    }

    @Override
    public RedstoneBoardRobotNBT getNBTHandler() {
        return BCRoboticsBoards.getByKey("stripes").nbt();
    }

    @Override
    public void update() {
        ItemStack held = robot.getItemBySlot(EquipmentSlot.MAINHAND);
        if (held.isEmpty()) {
            startDelegateAI(new AIRobotFetchAndEquipItemStack(robot, new AnyUsableStackFilter()));
            return;
        }

        if (blockFound != null) {
            startDelegateAI(new AIRobotStripesHandler(robot, blockFound));
            return;
        }

        startDelegateAI(new AIRobotSearchAndGotoBlock(robot, true, this::isFreeTargetBlock));
    }

    @Override
    public void delegateAIEnded(AIRobot ai) {
        if (ai instanceof AIRobotSearchAndGotoBlock search) {
            if (search.success()) {
                blockFound = search.getBlockFound();
                startDelegateAI(new AIRobotStripesHandler(robot, blockFound));
            } else {
                startDelegateAI(new AIRobotGotoSleep(robot));
            }
        } else if (ai instanceof AIRobotFetchAndEquipItemStack) {
            if (!ai.success()) {
                startDelegateAI(new AIRobotGotoSleep(robot));
            }
        } else if (ai instanceof AIRobotStripesHandler) {
            releaseBlockFound();
            if (!ai.success() && !robot.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
                startDelegateAI(new AIRobotGotoStationAndUnload(robot));
            }
        } else if (ai instanceof AIRobotGotoStationAndUnload) {
            if (!ai.success()) {
                startDelegateAI(new AIRobotGotoSleep(robot));
            }
        } else if (ai instanceof AIRobotGotoSleep) {
            terminate();
        }
    }

    private boolean isFreeTargetBlock(Level level, BlockPos pos) {
        if (!level.isLoaded(pos) || !level.isEmptyBlock(pos)) {
            return false;
        }
        return robot.getRegistry() == null || !robot.getRegistry().isTaken(new ResourceIdBlock(pos));
    }

    private void releaseBlockFound() {
        if (blockFound != null) {
            if (robot.getRegistry() != null) {
                robot.getRegistry().release(new ResourceIdBlock(blockFound));
            }
            blockFound = null;
        }
    }

    @Override
    public void end() {
        releaseBlockFound();
    }

    @Override
    public boolean canLoadFromNBT() {
        return true;
    }

    @Override
    public void writeSelfToNBT(CompoundTag nbt) {
        if (blockFound != null) {
            CompoundTag tag = new CompoundTag();
            blockFound.writeTo(tag);
            nbt.put("blockFound", tag);
        }
    }

    @Override
    public void loadSelfFromNBT(CompoundTag nbt) {
        if (nbt.contains("blockFound")) {
            blockFound = new BlockIndex(nbt.getCompound("blockFound"));
        }
    }

    private static class AnyUsableStackFilter implements IStackFilter {
        @Override
        public boolean matches(ItemStack stack) {
            return !stack.isEmpty();
        }
    }
}
