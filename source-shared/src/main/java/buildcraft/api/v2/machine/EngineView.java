package buildcraft.api.v2.machine;

import buildcraft.api.v2.energy.MjAmount;

public interface EngineView extends MachineView {
    EngineStage stage();
    MjAmount outputPerTick();
}
