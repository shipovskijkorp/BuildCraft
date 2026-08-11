package buildcraft.api.v2.machine;

import buildcraft.api.v2.energy.MjPort;

/** Target that accepts laser MJ through a dedicated, transaction-safe port. */
public interface LaserTarget {
    MjPort laserPort();
}
