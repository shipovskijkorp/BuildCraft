package buildcraft.robotics;

import buildcraft.api.robots.DockingStation;

@FunctionalInterface
public interface IStationFilter {
    boolean matches(DockingStation station);
}
