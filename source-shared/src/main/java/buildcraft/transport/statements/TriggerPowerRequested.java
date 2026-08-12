package buildcraft.transport.statements;

import javax.annotation.Nullable;

import buildcraft.api.core.render.ISprite;
import buildcraft.transport.internal.gate.IGate;
import buildcraft.lib.internal.statement.IStatementContainer;
import buildcraft.lib.internal.statement.IStatementParameter;
import buildcraft.lib.internal.statement.ITriggerInternal;
import buildcraft.transport.internal.pipe.IFlowForgeEnergy;
import buildcraft.transport.internal.pipe.PipeFlow;

import buildcraft.core.statements.BCStatement;
import buildcraft.transport.BCTransportSprites;
import buildcraft.transport.pipe.flow.PipeFlowPower;

import net.minecraft.network.chat.Component;

public class TriggerPowerRequested extends BCStatement implements ITriggerInternal {

    public TriggerPowerRequested() {
        super("buildcraft:powerRequested");
    }

    @Override
    public boolean isTriggerActive(IStatementContainer source, IStatementParameter[] parameters) {
        if (!(source instanceof IGate)) {
            return false;
        }
        PipeFlow f = ((IGate) source).getPipeHolder().getPipe().getFlow();
        if (f instanceof PipeFlowPower flow) {
            return flow.getPowerRequested(null) > 0;
        }
        if (f instanceof IFlowForgeEnergy flow) {
            return flow.getPowerRequested(null) > 0;
        }
        return false;
    }

    @Override
    public Component getDescription() {
        return Component.translatable("gate.trigger.pipe.requestsEnergy");
    }

    @Nullable
    @Override
    public ISprite getSprite() {
        return BCTransportSprites.TRIGGER_POWER_REQUESTED;
    }

}
