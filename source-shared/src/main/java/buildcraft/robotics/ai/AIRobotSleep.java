package buildcraft.robotics.ai;

import buildcraft.api.robots.AIRobot;
import buildcraft.api.robots.DockingStation;
import buildcraft.api.robots.EntityRobotBase;
import buildcraft.api.statements.StatementSlot;
import buildcraft.robotics.statements.ActionRobotWakeUp;
import net.minecraft.nbt.CompoundTag;

public class AIRobotSleep extends AIRobot {
    private static final int SLEEPING_TIME = 30 * 20;
    private int sleptTime;

    public AIRobotSleep(EntityRobotBase robot) {
        super(robot);
    }

    @Override
    public void preempt(AIRobot ai) {
        // A wake-up signal must not bounce a discharged robot back into work. Stay parked until the station has
        // restored the normal safety reserve.
        if (robot.getEnergy() < EntityRobotBase.SAFETY_ENERGY) {
            return;
        }

        DockingStation station = robot.getLinkedStation();
        if (station == null) {
            return;
        }

        for (StatementSlot slot : station.getActiveActions()) {
            if (slot.statement instanceof ActionRobotWakeUp) {
                terminate();
                return;
            }
        }
    }

    @Override
    public void update() {
        if (robot.getEnergy() < EntityRobotBase.SAFETY_ENERGY) {
            sleptTime = 0;
            return;
        }

        sleptTime++;
        if (sleptTime > SLEEPING_TIME) {
            terminate();
        }
    }

    @Override
    public int getEnergyCost() {
        if (robot.getEnergy() < EntityRobotBase.SAFETY_ENERGY) {
            return 0;
        }
        return sleptTime % 10 == 0 ? 1 : 0;
    }

    @Override
    public boolean canLoadFromNBT() {
        return true;
    }

    @Override
    public void writeSelfToNBT(CompoundTag nbt) {
        nbt.putInt("sleptTime", sleptTime);
    }

    @Override
    public void loadSelfFromNBT(CompoundTag nbt) {
        sleptTime = nbt.getInt("sleptTime");
    }
}
