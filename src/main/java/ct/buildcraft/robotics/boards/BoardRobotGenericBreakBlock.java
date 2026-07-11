package ct.buildcraft.robotics.boards;

import ct.buildcraft.api.core.IStackFilter;
import ct.buildcraft.api.robots.AIRobot;
import ct.buildcraft.api.robots.EntityRobotBase;
import ct.buildcraft.robotics.ai.AIRobotBreak;
import ct.buildcraft.robotics.ai.AIRobotFetchAndEquipItemStack;
import ct.buildcraft.robotics.ai.AIRobotGotoSleep;
import ct.buildcraft.robotics.ai.AIRobotGotoStationAndUnload;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public abstract class BoardRobotGenericBreakBlock extends BoardRobotGenericSearchBlock {
    public BoardRobotGenericBreakBlock(EntityRobotBase robot) {
        super(robot);
    }

    public abstract boolean isExpectedTool(ItemStack stack);

    @Override
    public void update() {
        ItemStack held = robot.getItemBySlot(EquipmentSlot.MAINHAND);
        boolean toolRequired = !isExpectedTool(ItemStack.EMPTY);
        if (toolRequired && !held.isEmpty() && !isExpectedTool(held)) {
            RobotBoardUtil.dropHeldItem(robot);
            return;
        }
        if (toolRequired && held.isEmpty()) {
            startDelegateAI(new AIRobotFetchAndEquipItemStack(robot, new ExpectedToolFilter()));
        } else if (!held.isEmpty() && held.isDamageableItem() && held.getDamageValue() >= held.getMaxDamage()) {
            startDelegateAI(new AIRobotGotoStationAndUnload(robot));
        } else if (blockFound() != null) {
            startDelegateAI(new AIRobotBreak(robot, blockFound()));
        } else {
            super.update();
        }
    }

    @Override
    public void delegateAIEnded(AIRobot ai) {
        if (ai instanceof AIRobotFetchAndEquipItemStack || ai instanceof AIRobotGotoStationAndUnload) {
            if (!ai.success()) {
                startDelegateAI(new AIRobotGotoSleep(robot));
            }
        } else if (ai instanceof AIRobotBreak) {
            releaseBlockFound(ai.success());
        }
        super.delegateAIEnded(ai);
    }

    private class ExpectedToolFilter implements IStackFilter {
        @Override
        public boolean matches(ItemStack stack) {
            return !stack.isEmpty()
                    && (!stack.isDamageableItem() || stack.getDamageValue() < stack.getMaxDamage())
                    && isExpectedTool(stack);
        }
    }
}
