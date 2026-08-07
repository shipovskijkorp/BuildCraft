package buildcraft.transport.statements;

import buildcraft.api.core.render.ISprite;
import buildcraft.api.gates.IGate;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.ITriggerInternal;
import buildcraft.api.statements.StatementParameterItemStack;
import buildcraft.core.statements.BCStatement;
import buildcraft.transport.BCTransportSprites;
import buildcraft.transport.pipe.flow.PipeFlowFluids;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

public class TriggerFluidsTraversing extends BCStatement implements ITriggerInternal {

    public TriggerFluidsTraversing() {
        super("buildcraft:pipe_contains_fluids");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("gate.trigger.pipe.containsFluids");
    }

    @Override
    public ISprite getSprite() {
        return BCTransportSprites.TRIGGER_FLUIDS_TRAVERSING;
    }

    @Override
    public boolean isTriggerActive(IStatementContainer source, IStatementParameter[] parameters) {
    	FluidStack searchedFluid = FluidStack.EMPTY;
        if (parameters != null && parameters.length >= 1 && parameters[0] != null) {
        	ItemStack searchedStack = parameters[0].getItemStack();
            searchedFluid = FluidUtil.getFluidContained(searchedStack).orElse(FluidStack.EMPTY);
        }
        return source instanceof IGate gate 
        		&& gate.getPipeHolder().getPipe().getFlow() instanceof PipeFlowFluids fluidflow
        		&& fluidflow.doesContainFluid(searchedFluid);
    }
    
    @Override
    public int maxParameters() {
        return 1;
    }
    
    @Override
    public IStatementParameter createParameter(int index) {
        return new StatementParameterItemStack();
    }
}
