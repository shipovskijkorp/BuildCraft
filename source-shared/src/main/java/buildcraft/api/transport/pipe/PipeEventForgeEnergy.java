package buildcraft.api.transport.pipe;

import net.minecraft.core.Direction;

/** Events specific to Forge Energy pipe flow. */
public abstract class PipeEventForgeEnergy extends PipeEvent {
    public final IFlowForgeEnergy flow;

    protected PipeEventForgeEnergy(IPipeHolder holder, IFlowForgeEnergy flow) {
        super(holder);
        this.flow = flow;
    }

    public static class Configure extends PipeEventForgeEnergy {
        private int maxPower = 100;
        private boolean receiver;
        private boolean transferDisabled;

        public Configure(IPipeHolder holder, IFlowForgeEnergy flow) {
            super(holder, flow);
        }

        public int getMaxPower() {
            return maxPower;
        }

        public void setMaxPower(int maxPower) {
            this.maxPower = maxPower;
        }

        public boolean isReceiver() {
            return receiver;
        }

        public void setReceiver(boolean receiver) {
            this.receiver = receiver;
        }

        public void disableTransfer() {
            transferDisabled = true;
        }

        public boolean isTransferDisabled() {
            return transferDisabled;
        }
    }

    public static class PrimaryDirection extends PipeEventForgeEnergy {
        private Direction facing;

        public PrimaryDirection(IPipeHolder holder, IFlowForgeEnergy flow, Direction facing) {
            super(holder, flow);
            this.facing = facing;
        }

        public Direction getFacing() {
            return facing;
        }

        public void setFacing(Direction facing) {
            this.facing = facing;
        }
    }
}
