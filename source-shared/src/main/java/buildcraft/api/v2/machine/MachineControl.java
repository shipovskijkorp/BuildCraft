package buildcraft.api.v2.machine;

import buildcraft.api.v2.OperationMode;
import java.util.Set;

public interface MachineControl {
    MachineControlMode mode();
    Set<MachineControlMode> supportedModes();
    boolean setMode(MachineControlMode mode, OperationMode operationMode);

    default boolean setEnabled(boolean enabled, OperationMode operationMode) {
        MachineControlMode target = enabled ? MachineControlMode.ON : MachineControlMode.OFF;
        return supportedModes().contains(target) && setMode(target, operationMode);
    }
}
