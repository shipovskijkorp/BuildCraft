package ct.buildcraft.robotics;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import ct.buildcraft.api.robots.DockingStation;
import ct.buildcraft.api.robots.EntityRobotBase;
import ct.buildcraft.api.robots.IRobotRegistry;
import ct.buildcraft.api.robots.IRobotRegistryProvider;
import ct.buildcraft.api.robots.ResourceId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

public enum SimpleRobotRegistryProvider implements IRobotRegistryProvider {
    INSTANCE;

    private final Map<Level, SimpleRobotRegistry> registries = new WeakHashMap<>();

    @Override
    public IRobotRegistry getRegistry(Level level) {
        return registries.computeIfAbsent(level, SimpleRobotRegistry::new);
    }

    private static final class SimpleRobotRegistry implements IRobotRegistry {
        private final Level level;
        private final Map<Long, EntityRobotBase> robots = new HashMap<>();
        private final Map<ResourceId, Long> resources = new HashMap<>();
        private final Map<StationKey, DockingStation> stations = new LinkedHashMap<>();
        private long nextRobotId = 1;

        private SimpleRobotRegistry(Level level) {
            this.level = level;
        }

        @Override
        public long getNextRobotId() {
            return nextRobotId++;
        }

        @Override
        public void registerRobot(EntityRobotBase robot) {
            if (robot != null && robot.getRobotId() != EntityRobotBase.NULL_ROBOT_ID) {
                robots.put(robot.getRobotId(), robot);
            }
        }

        @Override
        public void killRobot(EntityRobotBase robot) {
            if (robot != null) {
                releaseResources(robot);
                releaseStations(robot, true, false);
                robots.remove(robot.getRobotId());
            }
        }

        @Override
        public void unloadRobot(EntityRobotBase robot) {
            if (robot != null) {
                releaseStations(robot, false, true);
                robots.remove(robot.getRobotId());
            }
        }

        @Override
        public EntityRobotBase getLoadedRobot(long id) {
            return robots.get(id);
        }

        @Override
        public boolean isTaken(ResourceId resourceId) {
            return resources.containsKey(resourceId);
        }

        @Override
        public long robotIdTaking(ResourceId resourceId) {
            return resources.getOrDefault(resourceId, EntityRobotBase.NULL_ROBOT_ID);
        }

        @Override
        public EntityRobotBase robotTaking(ResourceId resourceId) {
            return robots.get(robotIdTaking(resourceId));
        }

        @Override
        public boolean take(ResourceId resourceId, EntityRobotBase robot) {
            if (robot == null) return false;
            return take(resourceId, robot.getRobotId());
        }

        @Override
        public boolean take(ResourceId resourceId, long robotId) {
            Long current = resources.get(resourceId);
            if (current == null || current == robotId) {
                resources.put(resourceId, robotId);
                return true;
            }
            return false;
        }

        @Override
        public void release(ResourceId resourceId) {
            resources.remove(resourceId);
        }

        @Override
        public void releaseResources(EntityRobotBase robot) {
            if (robot == null) return;
            resources.entrySet().removeIf(entry -> entry.getValue() == robot.getRobotId());
            releaseStations(robot, false, false);
        }

        private void releaseStations(EntityRobotBase robot, boolean forceAll, boolean resetEntities) {
            if (robot == null) return;
            long robotId = robot.getRobotId();
            for (DockingStation station : stations.values()) {
                if (station == null || station.robotIdTaking() != robotId) {
                    continue;
                }

                if (forceAll || station.canRelease()) {
                    station.unsafeRelease(robot);
                    resources.remove(new StationResourceId(station), robotId);
                } else if (resetEntities) {
                    station.invalidateRobotTakingEntity();
                }
            }
        }

        @Override
        public DockingStation getStation(int x, int y, int z, @Nullable Direction side) {
            return stations.get(new StationKey(new BlockPos(x, y, z), side));
        }

        @Override
        public Collection<DockingStation> getStations() {
            return Collections.unmodifiableCollection(stations.values());
        }

        @Override
        public void registerStation(DockingStation station) {
            if (station == null) return;
            station.setLevel(level);
            stations.put(new StationKey(new BlockPos(station.x(), station.y(), station.z()), station.side()), station);
        }

        @Override
        public void removeStation(DockingStation station) {
            if (station == null) return;
            EntityRobotBase robot = station.robotTaking();
            if (robot != null) {
                if (station.isMainStation()) {
                    robot.setMainStation(null);
                } else {
                    robot.undock();
                }
                station.unsafeRelease(robot);
            }
            resources.entrySet().removeIf(entry -> entry.getKey() instanceof StationResourceId id
                    && id.matches(station));
            stations.remove(new StationKey(new BlockPos(station.x(), station.y(), station.z()), station.side()));
        }

        @Override
        public void take(DockingStation station, long robotId) {
            if (station != null) resources.put(new StationResourceId(station), robotId);
        }

        @Override
        public void release(DockingStation station, long robotId) {
            if (station != null) resources.remove(new StationResourceId(station), robotId);
        }

        @Override
        public void writeToNBT(CompoundTag nbt) {
            nbt.putLong("nextRobotId", nextRobotId);
        }

        @Override
        public void readFromNBT(CompoundTag nbt) {
            nextRobotId = Math.max(1, nbt.getLong("nextRobotId"));
        }

        @Override
        public void registryMarkDirty() {
        }
    }

    private record StationKey(BlockPos pos, @Nullable Direction side) {
    }

    private static final class StationResourceId extends ResourceId {
        private final BlockPos pos;
        private final Direction side;

        private StationResourceId(DockingStation station) {
            this.pos = new BlockPos(station.x(), station.y(), station.z());
            this.side = station.side();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof StationResourceId other)) return false;
            return pos.equals(other.pos) && side == other.side;
        }

        private boolean matches(DockingStation station) {
            return station != null
                    && pos.equals(new BlockPos(station.x(), station.y(), station.z()))
                    && side == station.side();
        }

        @Override
        public int hashCode() {
            return 31 * pos.hashCode() + (side == null ? 0 : side.hashCode());
        }
    }
}
