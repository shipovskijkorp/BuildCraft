package buildcraft.api.v2.gate;

import buildcraft.api.v2.OperationMode;

public interface GateControl {
    boolean setProgram(GateProgram program, OperationMode mode);
}
