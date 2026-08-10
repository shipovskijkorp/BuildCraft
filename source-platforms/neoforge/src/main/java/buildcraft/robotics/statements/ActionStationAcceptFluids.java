package buildcraft.robotics.statements;

import buildcraft.api.statements.IActionInternal;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.StatementParameterItemStack;
import buildcraft.core.statements.BCStatement;
import buildcraft.api.core.render.ISprite;
import buildcraft.robotics.BCRoboticsSprites;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ActionStationAcceptFluids extends BCStatement implements IActionInternal {

    public ActionStationAcceptFluids() {
        super("buildcraft:station.accept_fluids");
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
        return Component.translatable("gate.action.station.acceptFluids");
    }

    @Override
    public void actionActivate(IStatementContainer container, IStatementParameter[] parameters) {}

    @Override
    @OnlyIn(Dist.CLIENT)
    public ISprite getSprite() {
        return BCRoboticsSprites.ACTION_STATION_ACCEPT_FLUIDS;
    }
}
