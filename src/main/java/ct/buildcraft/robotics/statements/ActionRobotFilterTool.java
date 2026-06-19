package ct.buildcraft.robotics.statements;

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

public class ActionRobotFilterTool extends BCStatement implements IActionInternal {

    public ActionRobotFilterTool() {
        super("buildcraft:robot.work_filter_tool");
    }

    @Override
    public int maxParameters() { return 1; }
    @Override
    public int minParameters() { return 1; }

    @Override
    public IStatementParameter createParameter(int index) {
        return StatementParameterItemStack.EMPTY;
    }

    @Override
    public Component getDescription() {
        return Component.translatable("gate.action.robot.filterTool");
    }

    @Override
    public void actionActivate(IStatementContainer container, IStatementParameter[] parameters) {}

    public static ItemStack getToolFilter(IStatementParameter[] parameters) {
        if (parameters == null || parameters.length == 0) return ItemStack.EMPTY;
        IStatementParameter p = parameters[0];
        if (p instanceof StatementParameterItemStack s) return s.getItemStack();
        return ItemStack.EMPTY;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public SpriteHolder getSprite() {
        return BCRoboticsSprites.ACTION_ROBOT_FILTER_TOOL;
    }
}
