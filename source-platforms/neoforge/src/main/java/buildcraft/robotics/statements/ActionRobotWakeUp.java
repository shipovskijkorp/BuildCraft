package buildcraft.robotics.statements;

import buildcraft.api.robots.DockingStation;
import buildcraft.api.robots.EntityRobotBase;
import buildcraft.api.statements.IActionInternal;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.core.statements.BCStatement;
import buildcraft.api.core.render.ISprite;
import buildcraft.robotics.BCRoboticsSprites;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ActionRobotWakeUp extends BCStatement implements IActionInternal {

    public ActionRobotWakeUp() {
        super("buildcraft:robot.wakeup");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("gate.action.robot.wakeUp");
    }

    @Override
    public void actionActivate(IStatementContainer container, IStatementParameter[] parameters) {
        DockingStation station = RobotStatementUtils.getStation(container);
        if (station == null) return;
        EntityRobotBase robot = station.robotTaking();
        if (robot == null) return;
        // Clear sleep override so robot resumes normal AI
        robot.setMainAIOverride(null);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ISprite getSprite() {
        return BCRoboticsSprites.ACTION_ROBOT_WAKEUP;
    }
}
