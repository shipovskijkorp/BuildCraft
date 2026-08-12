package buildcraft.transport.statements;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import buildcraft.lib.internal.core.render.ISprite;
import buildcraft.transport.internal.gate.IGate;
import buildcraft.lib.internal.statement.IStatementContainer;
import buildcraft.lib.internal.statement.IStatementParameter;
import buildcraft.lib.internal.statement.ITriggerInternal;
import buildcraft.lib.internal.statement.StatementParameterItemStack;
import buildcraft.transport.internal.pipe.PipeFlow;

import buildcraft.core.statements.BCStatement;
import buildcraft.transport.BCTransportSprites;
import buildcraft.transport.pipe.flow.PipeFlowItems;

public class TriggerItemsTraversing extends BCStatement implements ITriggerInternal {

    public TriggerItemsTraversing() {
        super("buildcraft:pipe_contains_items");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("gate.trigger.pipe.containsItems");
    }

    @Override
    public ISprite getSprite() {
        return BCTransportSprites.TRIGGER_ITEMS_TRAVERSING;
    }

    @Override
    public int maxParameters() {
        return 1;
    }

    @Override
    public IStatementParameter createParameter(int index) {
        return StatementParameterItemStack.EMPTY;
    }

    @Override
    public boolean isTriggerActive(IStatementContainer source, IStatementParameter[] parameters) {
        if (source instanceof IGate) {
            PipeFlow flow = ((IGate) source).getPipeHolder().getPipe().getFlow();
            if (flow instanceof PipeFlowItems) {
                PipeFlowItems itemFlow = (PipeFlowItems) flow;

                ItemStack filter = getParam(0, parameters, StatementParameterItemStack.EMPTY).getItemStack();
                return itemFlow.containsItemMatching(filter);
            }
        }
        return false;
    }
}
