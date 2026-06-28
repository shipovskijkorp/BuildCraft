package ct.buildcraft.robotics.ai;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import ct.buildcraft.api.core.BlockIndex;
import ct.buildcraft.api.core.IZone;
import ct.buildcraft.api.robots.AIRobot;
import ct.buildcraft.api.robots.EntityRobotBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7.10 BuildCraft block search port. It searches reachable soft blocks and returns a path to a soft block adjacent to
 * the matching target, mirroring the old PathFindingSearch + path.removeLast() robotics flow.
 */
public class AIRobotSearchBlock extends AIRobot {
    private static final int DEFAULT_RANGE = 96;
    private static final int MAX_VISITED = DEFAULT_RANGE * DEFAULT_RANGE * 8;
    private static final int NODES_PER_TICK = 512;

    public BlockIndex blockFound;
    public LinkedList<BlockIndex> path;

    private IBlockFilter filter;
    private boolean random;
    private double maxDistanceToEnd;
    private IZone zone;
    private boolean searched;
    private BlockPos start;
    private int hardLimit;
    private Queue<BlockPos> queue;
    private Set<BlockPos> seen;
    private Map<BlockPos, BlockPos> parent;

    public AIRobotSearchBlock(EntityRobotBase robot) {
        super(robot);
    }

    public AIRobotSearchBlock(EntityRobotBase robot, boolean random, IBlockFilter filter, double maxDistanceToEnd) {
        this(robot);
        this.random = random;
        this.filter = filter;
        this.maxDistanceToEnd = maxDistanceToEnd;
        this.zone = robot.getZoneToWork();
    }

    @Override
    public void update() {
        if (filter == null) {
            setSuccess(false);
            terminate();
            return;
        }

        if (!searched) {
            beginSearch();
        }

        SearchResult result = continueSearch();
        if (result != null) {
            blockFound = new BlockIndex(result.target());
            path = result.path();
            terminate();
        } else if (queue == null || queue.isEmpty() || seen.size() >= MAX_VISITED) {
            setSuccess(false);
            terminate();
        }
    }

    private void beginSearch() {
        searched = true;
        start = robot.blockPosition();
        hardLimit = maxDistanceToEnd > 0 ? Math.max(1, (int) Math.ceil(maxDistanceToEnd)) : DEFAULT_RANGE;
        queue = new ArrayDeque<>();
        seen = new HashSet<>();
        parent = new HashMap<>();
        queue.add(start);
        seen.add(start);
    }

    private SearchResult continueSearch() {
        Level level = robot.level;
        int processed = 0;
        while (queue != null && !queue.isEmpty() && seen.size() < MAX_VISITED && processed++ < NODES_PER_TICK) {
            BlockPos current = queue.remove();
            SearchResult adjacent = findAdjacentTarget(level, start, current, parent);
            if (adjacent != null) {
                return adjacent;
            }

            List<Direction> directions = directions();
            for (Direction direction : directions) {
                BlockPos next = current.relative(direction);
                if (seen.contains(next)) continue;
                if (distanceSqr(start, next) > hardLimit * hardLimit) continue;
                if (zone != null && !zone.contains(center(next))) continue;
                if (!isSoft(level, next)) continue;
                parent.put(next, current);
                seen.add(next);
                queue.add(next);
            }
        }
        return null;
    }

    private SearchResult findAdjacentTarget(Level level, BlockPos start, BlockPos current, Map<BlockPos, BlockPos> parent) {
        for (Direction direction : directions()) {
            BlockPos target = current.relative(direction);
            if (!level.isLoaded(target)) continue;
            if (zone != null && !zone.contains(center(target))) continue;
            if (filter.matches(level, target)) {
                return new SearchResult(target, reconstruct(start, current, parent));
            }
        }
        return null;
    }

    private List<Direction> directions() {
        List<Direction> directions = new ArrayList<>(List.of(Direction.values()));
        if (random) {
            Collections.shuffle(directions);
        }
        return directions;
    }

    private static Vec3 center(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    private static LinkedList<BlockIndex> reconstruct(BlockPos start, BlockPos target, Map<BlockPos, BlockPos> parent) {
        List<BlockPos> reversed = new ArrayList<>();
        BlockPos current = target;
        while (current != null && !current.equals(start)) {
            reversed.add(current);
            current = parent.get(current);
        }
        Collections.reverse(reversed);
        LinkedList<BlockIndex> out = new LinkedList<>();
        for (BlockPos pos : reversed) {
            out.add(new BlockIndex(pos));
        }
        return out;
    }

    private static double distanceSqr(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private static boolean isSoft(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) return false;
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.getCollisionShape(level, pos).isEmpty();
    }

    @Override
    public boolean success() {
        return blockFound != null;
    }

    @Override
    public boolean canLoadFromNBT() {
        return true;
    }

    @Override
    public void writeSelfToNBT(CompoundTag nbt) {
        if (blockFound != null) {
            CompoundTag tag = new CompoundTag();
            blockFound.writeTo(tag);
            nbt.put("blockFound", tag);
        }
    }

    @Override
    public void loadSelfFromNBT(CompoundTag nbt) {
        if (nbt.contains("blockFound")) {
            blockFound = new BlockIndex(nbt.getCompound("blockFound"));
        }
    }

    public boolean takeResource() {
        return blockFound != null && robot.getRegistry() != null
                && robot.getRegistry().take(new ct.buildcraft.api.robots.ResourceIdBlock(blockFound), robot);
    }

    public void unreserve() {
        if (blockFound != null && robot.getRegistry() != null) {
            robot.getRegistry().release(new ct.buildcraft.api.robots.ResourceIdBlock(blockFound));
        }
    }

    @Override
    public int getEnergyCost() {
        return 2;
    }

    private record SearchResult(BlockPos target, LinkedList<BlockIndex> path) {
    }
}
