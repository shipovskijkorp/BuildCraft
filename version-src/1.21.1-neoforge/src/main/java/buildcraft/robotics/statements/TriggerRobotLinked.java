package buildcraft.robotics.statements;

import buildcraft.api.robots.DockingStation;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.ITriggerInternal;
import buildcraft.core.statements.BCStatement;
import buildcraft.api.core.render.ISprite;
import buildcraft.robotics.BCRoboticsSprites;
import buildcraft.robotics.BCRoboticsStatements;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class TriggerRobotLinked extends BCStatement implements ITriggerInternal {

    public final boolean reserved;

    public TriggerRobotLinked(boolean reserved) {
        super(reserved ? "buildcraft:robot.reserved" : "buildcraft:robot.linked");
        this.reserved = reserved;
    }

    @Override
    public Component getDescription() {
        return Component.translatable(reserved ? "gate.trigger.robot.reserved" : "gate.trigger.robot.linked");
    }

    @Override
    public boolean isTriggerActive(IStatementContainer container, IStatementParameter[] parameters) {
        DockingStation station = RobotStatementUtils.getStation(container);
        if (station == null) return false;
        return station.isTaken() && (reserved || station.isMainStation());
    }

    @Override
    public IStatement[] getPossible() {
        return new IStatement[] {
            BCRoboticsStatements.TRIGGER_ROBOT_LINKED,
            BCRoboticsStatements.TRIGGER_ROBOT_RESERVED
        };
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ISprite getSprite() {
        return reserved ? BCRoboticsSprites.TRIGGER_ROBOT_RESERVED : BCRoboticsSprites.TRIGGER_ROBOT_LINKED;
    }
}
