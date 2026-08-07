package buildcraft.lib.block.fluid;

import buildcraft.lib.misc.FluidStackUtil;
import buildcraft.lib.fluid.FluidCompatRegistry;

import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;


public class BCTank implements IFluidHandler,IFluidTank{

	protected int tanks = 1;
	protected FluidStack fluid = FluidStack.EMPTY;
	protected int capacity;
	public BCTank(int capacity,Fluid flu,int amount) {
		this.fluid = FluidCompatRegistry.canonicalize(new FluidStack(flu,amount));
		this.capacity = capacity;
		this.setTanks((int)(capacity/8000));
	}
	public BCTank(int capacity) {
		this.setTanks((int)(capacity/8000));
		this.capacity = capacity;
	}

	public void declineAboveTank(int tank_num) {
		int newTankCapacity = getCapacity() - tank_num * 8000;
		if(!fluid.isEmpty()&&getFluidAmount()>newTankCapacity) fluid.setAmount(newTankCapacity);
		capacity = newTankCapacity;
		setTanks(getTanks() - tank_num);
	}
	public int getCapacity() {
		return capacity;
	}
	public Boolean tryAddTank(BCTank subtank) {
		if(subtank.isEmpty()) {
			this.setCapacity(this.getCapacity() + subtank.getCapacity());
			setTanks(getTanks() + subtank.getTanks());
		}
		else if(fluid.isEmpty()) {
			this.setCapacity(this.getCapacity() + subtank.getCapacity());
			this.fluid = FluidCompatRegistry.canonicalize(subtank.fluid);
			setTanks(getTanks() + subtank.getTanks());
		}
		else if(FluidCompatRegistry.areEquivalent(fluid, subtank.getFluid())) {
			this.setCapacity(this.getCapacity() + subtank.getCapacity());
			fluid.setAmount(subtank.getFluidAmount() + fluid.getAmount());
			setTanks(getTanks() + subtank.getTanks());
		}
		else return false;
		
		return true;
	}
	public @NotNull FluidStack getFluid() {
		return fluid;
	}
	public int getAmountOnNUM(int num) {
		int i = getFluidAmount() - (getTanks()-num) * 8000;
		if(i>8000)
			return 8000;
		else if(i<0)
			return 0;
		else 
			return i;
	}
	public boolean isEmpty() {
		return fluid.isEmpty();
	}
    public BCTank setCapacity(int capacity)
    {
        this.capacity = capacity;
        return this;
    }
	@Override
	public int getTanks() {
		return tanks;
	}
	@Override
	public @NotNull FluidStack getFluidInTank(int tank) {
		int i = fluid.getAmount() - tank*8000;
		if(i<0)
			return FluidStack.EMPTY;
		else if(i>8000)
			return fluid.copyWithAmount(8000);
		return fluid.copyWithAmount(i);
	}
	@Override
	public int getTankCapacity(int tank) {
		return capacity;
	}
	@Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return fluid.isEmpty() || FluidCompatRegistry.areEquivalent(stack, fluid);
    }
	   @Override
	    public int fill(FluidStack resource, FluidAction action)
	    {
            resource = FluidCompatRegistry.canonicalize(resource);
	        if (resource.isEmpty() || !isFluidValid(resource))
	        {
	            return 0;
	        }
	        if (action.simulate())
	        {
	            if (fluid.isEmpty())
	            {
	                return Math.min(capacity, resource.getAmount());
	            }
	            if (!FluidCompatRegistry.areEquivalent(fluid, resource))
	            {
	                return 0;
	            }
	            return Math.min(capacity - fluid.getAmount(), resource.getAmount());
	        }
	        if (fluid.isEmpty())
	        {
	            fluid = resource.copyWithAmount(Math.min(capacity, resource.getAmount()));
	            return fluid.getAmount();
	        }
	        if (!FluidCompatRegistry.areEquivalent(fluid, resource))
	        {
	            return 0;
	        }
	        int filled = capacity - fluid.getAmount();

	        if (resource.getAmount() < filled)
	        {
	            fluid.grow(resource.getAmount());
	            filled = resource.getAmount();
	        }
	        else
	        {
	            fluid.setAmount(capacity);
	        }
	     
	        return filled;
	    }
	@Override
	public boolean isFluidValid(FluidStack resource) {
		
		return isEmpty()?true:FluidCompatRegistry.areEquivalent(resource, fluid);
	}
    @NotNull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action)
    {
        if (resource.isEmpty() || !FluidCompatRegistry.areEquivalent(resource, fluid))
        {
            return FluidStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
    }

    @NotNull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action)
    {
        int drained = maxDrain;
        if (fluid.getAmount() < drained)
        {
            drained = fluid.getAmount();
        }
        FluidStack stack = fluid.copyWithAmount(drained);
        if (action.execute() && drained > 0)
        {
            fluid.shrink(drained);
        }
        return stack;
    }
	public void setTanks(int tanks) {
		this.tanks = tanks;
	}
	public int getFluidAmount() {
		return fluid.getAmount();
	}
	public void writeToNBT(CompoundTag tag) {
		tag.putInt("tanks", tanks);
		tag.putInt("capacity", capacity);
		tag.merge(FluidStackUtil.saveOptional(fluid));
//		LogUtils.getLogger().info(Integer.toString(tag.getInt("Amount")));
	}
	public void readFromNBT(CompoundTag tag) {
		tanks = tag.getInt("tanks");
		capacity = tag.getInt("capacity");
		fluid = FluidCompatRegistry.canonicalize(FluidStackUtil.parseOptional(tag));
//		LogUtils.getLogger().info(Integer.toString(tag.getInt("tanks"))+"tanks");
	}
	
	
    	
}
