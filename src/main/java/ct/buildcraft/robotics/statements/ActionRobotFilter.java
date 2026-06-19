package ct.buildcraft.robotics.statements;

import ct.buildcraft.api.robots.EntityRobotBase;
import ct.buildcraft.api.statements.IActionInternal;
import ct.buildcraft.api.statements.IStatementContainer;
import ct.buildcraft.api.statements.IStatementParameter;
import ct.buildcraft.api.statements.StatementParameterItemStack;
import ct.buildcraft.core.statements.BCStatement;
import ct.buildcraft.lib.client.sprite.SpriteHolderRegistry.SpriteHolder;
import ct.buildcraft.robotics.BCRoboticsSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ActionRobotFilter extends BCStatement implements IActionInternal {

    public ActionRobotFilter() {
        super("buildcraft:robot.work_filter");
    }

    @Override
    public int maxParameters() { return 3; }
    @Override
    public int minParameters() { return 1; }

    @Override
    public IStatementParameter createParameter(int index) {
        return StatementParameterItemStack.EMPTY;
    }

    @Override
    public Component getDescription() {
        return Component.translatable("gate.action.robot.filter");
    }

    @Override
    public void actionActivate(IStatementContainer container, IStatementParameter[] parameters) {
        // Filter is used passively by the robot AI, not actively
    }

    /** Check if any parameter slot matches the given stack. */
    public static boolean matches(IStatementParameter[] parameters, ItemStack stack) {
        if (parameters == null) return true;
        boolean hasFilter = false;
        for (IStatementParameter p : parameters) {
            if (p instanceof StatementParameterItemStack s) {
                if (!s.getItemStack().isEmpty()) {
                    hasFilter = true;
                    if (ItemStack.isSameItemSameTags(s.getItemStack(), stack)) return true;
                }
            }
        }
        return !hasFilter;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public SpriteHolder getSprite() {
        return BCRoboticsSprites.ACTION_ROBOT_FILTER;
    }
}
