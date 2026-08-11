package buildcraft.api.v2.machine;

import buildcraft.api.v2.OperationMode;

/** Read/write heat endpoint replacing the legacy IHeatable capability. */
public interface HeatPort {
    HeatState heatState();
    HeatProfile profile();

    /** Requests an absolute temperature and returns the resulting state. */
    HeatState setTemperature(double temperature, OperationMode mode);
}
