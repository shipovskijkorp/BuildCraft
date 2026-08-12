package buildcraft.robotics;

import buildcraft.robotics.internal.legacy.robots.DockingStation;

@FunctionalInterface
public interface IStationFilter {
    boolean matches(DockingStation station);
}
