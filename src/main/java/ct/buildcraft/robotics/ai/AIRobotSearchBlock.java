package ct.buildcraft.robotics.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import ct.buildcraft.api.core.BlockIndex;
import ct.buildcraft.api.core.IBox;
import ct.buildcraft.api.core.IZone;
import ct.buildcraft.robotics.zone.ZonePlan;
import ct.buildcraft.api.robots.AIRobot;
import ct.buildcraft.api.robots.EntityRobotBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Searches for a matching work block and returns a path to a soft block adjacent to it.
 *
 * <p>The first robotics port used a pure reachable-air BFS and checked only blocks adjacent to every visited air node.
 * In open areas that meant the search budget was spent on the volume of air around the robot before it ever reached
 * trees a few chunks away. The original BuildCraft logic did the opposite: scan candidate blocks in an expanding area,
 * then pathfind only to real targets. This implementation keeps that behavior while using the modern soft-block checks.</p>
 */
public class AIRobotSearchBlock extends AIRobot {
    private static final int DEFAULT_RANGE = 96;
    private static final int MARKER_ZONE_MAX_EXACT_SIZE = 65;
    private static final int MAX_ASTAR_VISITED = 262144;

    public BlockIndex blockFound;
    public LinkedList<BlockIndex> path;

    private IBlockFilter filter;
    private boolean random;
    private double maxDistanceToEnd;
    private IZone zone;
    private boolean searched;
    private BlockPos start;
    private int searchRange;
    private CandidateScanner scanner;

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
        } else if (scanner == null || scanner.isDone()) {
            setSuccess(false);
            terminate();
        }
    }

    private void beginSearch() {
        searched = true;
        start = robot.blockPosition();
        searchRange = DEFAULT_RANGE;
        if (maxDistanceToEnd > 0) {
            searchRange = Math.max(searchRange, (int) Math.ceil(maxDistanceToEnd) + 8);
        }
        scanner = new CandidateScanner(robot.level, start, searchRange, random, zone);
    }

    private SearchResult continueSearch() {
        while (scanner != null && !scanner.isDone()) {
            BlockPos candidate = scanner.next();
            if (candidate == null) {
                break;
            }

            SearchResult result = evaluateCandidate(candidate);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private SearchResult evaluateCandidate(BlockPos target) {
        Level level = robot.level;
        if (!level.isLoaded(target)) {
            return null;
        }
        if (zone != null && !zone.contains(center(target))) {
            return null;
        }
        if (!filter.matches(level, target)) {
            return null;
        }

        List<BlockPos> adjacentPositions = adjacentSoftTargets(level, target);
        if (adjacentPositions.isEmpty()) {
            return null;
        }
        if (random) {
            Collections.shuffle(adjacentPositions);
        }

        LinkedList<BlockIndex> bestPath = null;
        for (BlockPos adjacent : adjacentPositions) {
            LinkedList<BlockIndex> candidatePath = pathToAdjacent(level, adjacent);
            if (candidatePath != null && (bestPath == null || candidatePath.size() < bestPath.size())) {
                bestPath = candidatePath;
            }
        }
        return bestPath == null ? null : new SearchResult(target, bestPath);
    }

    private List<BlockPos> adjacentSoftTargets(Level level, BlockPos target) {
        List<BlockPos> result = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            BlockPos adjacent = target.relative(direction);
            // Match the original BuildCraft behaviour: the work zone constrains the target block, not the
            // navigation corridor. A station/robot may legitimately start outside the zone and fly through
            // non-zone air to reach a block inside it, and edge blocks may only have a reachable soft side just
            // outside the zone.
            if (isSoft(level, adjacent)) {
                result.add(adjacent);
            }
        }
        return result;
    }

    private LinkedList<BlockIndex> pathToAdjacent(Level level, BlockPos target) {
        if (start.equals(target)) {
            return new LinkedList<>();
        }

        LinkedList<BlockIndex> direct = directFallback(start, target, level);
        if (direct != null) {
            return direct;
        }
        return findAStarPath(start, target, level);
    }

    private LinkedList<BlockIndex> findAStarPath(BlockPos start, BlockPos target, Level level) {
        int hardLimit = Math.max(searchRange + 16, (int) Math.ceil(Math.sqrt(distanceSqr(start, target))) + 8);
        int hardLimitSqr = hardLimit * hardLimit;
        PriorityQueue<PathNode> open = new PriorityQueue<>((a, b) -> {
            int score = Double.compare(a.score(), b.score());
            return score != 0 ? score : Long.compare(a.sequence(), b.sequence());
        });
        Map<BlockPos, BlockPos> parent = new HashMap<>();
        Map<BlockPos, Integer> bestCost = new HashMap<>();
        Set<BlockPos> closed = new HashSet<>();
        long sequence = 0;

        bestCost.put(start, 0);
        open.add(new PathNode(start, heuristic(start, target), sequence++));

        while (!open.isEmpty() && closed.size() < MAX_ASTAR_VISITED) {
            PathNode node = open.poll();
            BlockPos current = node.pos();
            if (!closed.add(current)) {
                continue;
            }
            if (current.equals(target)) {
                return reconstruct(start, target, parent);
            }

            int currentCost = bestCost.getOrDefault(current, Integer.MAX_VALUE);
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (closed.contains(next)) continue;
                if (distanceSqr(next, start) > hardLimitSqr) continue;
                // Do not clamp the path itself to the work zone. Only the searched target block is zone-limited.
                if (!isSoft(level, next)) continue;

                int newCost = currentCost + 1;
                if (newCost < bestCost.getOrDefault(next, Integer.MAX_VALUE)) {
                    bestCost.put(next, newCost);
                    parent.put(next, current);
                    open.add(new PathNode(next, newCost + heuristic(next, target), sequence++));
                }
            }
        }
        return null;
    }

    private static LinkedList<BlockIndex> directFallback(BlockPos start, BlockPos target, Level level) {
        int[][] orders = {
                {0, 1, 2}, {0, 2, 1},
                {1, 0, 2}, {1, 2, 0},
                {2, 0, 1}, {2, 1, 0}
        };
        for (int[] order : orders) {
            LinkedList<BlockIndex> out = traceAxisPath(start, target, level, order);
            if (out != null) {
                return out;
            }
        }
        return null;
    }

    private static LinkedList<BlockIndex> traceAxisPath(BlockPos start, BlockPos target, Level level, int[] order) {
        LinkedList<BlockIndex> out = new LinkedList<>();
        int[] current = {start.getX(), start.getY(), start.getZ()};
        int[] end = {target.getX(), target.getY(), target.getZ()};

        for (int axis : order) {
            while (current[axis] != end[axis]) {
                current[axis] += Integer.compare(end[axis], current[axis]);
                BlockPos pos = new BlockPos(current[0], current[1], current[2]);
                if (!isSoft(level, pos)) {
                    return null;
                }
                out.add(new BlockIndex(pos));
            }
        }
        return out;
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

    private static boolean isSoft(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) return false;
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.getCollisionShape(level, pos).isEmpty();
    }

    private static Vec3 center(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    private static double distanceSqr(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private static double heuristic(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY()) + Math.abs(a.getZ() - b.getZ());
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

    private record PathNode(BlockPos pos, double score, long sequence) {
    }

    private static final class CandidateScanner {
        private final Level level;
        private final BlockPos start;
        private final boolean random;
        private final ZonePlan zonePlan;
        private final ScanBounds bounds;
        private final int centerChunkX;
        private final int centerChunkZ;
        private final int maxRing;

        private int ring;
        private List<ScanArea> currentRingAreas;
        private int areaIndex;
        private AreaIterator currentArea;
        private boolean done;

        private CandidateScanner(Level level, BlockPos start, int range, boolean random, IZone zone) {
            this.level = level;
            this.start = start;
            this.random = random;
            this.zonePlan = zone instanceof ZonePlan plan ? plan : null;
            this.bounds = createBounds(level, start, range, zone);
            this.centerChunkX = start.getX() >> 4;
            this.centerChunkZ = start.getZ() >> 4;

            if (bounds == null || bounds.isEmpty()) {
                this.maxRing = -1;
                this.done = true;
                return;
            }

            this.maxRing = bounds.maxRingFrom(centerChunkX, centerChunkZ);
            this.ring = 0;
            buildRing();
        }

        private boolean isDone() {
            return done;
        }

        private BlockPos next() {
            while (!done) {
                if (currentArea != null) {
                    BlockPos next = currentArea.next();
                    if (next != null) {
                        return next;
                    }
                    currentArea = null;
                }

                if (currentRingAreas != null && areaIndex < currentRingAreas.size()) {
                    currentArea = new AreaIterator(currentRingAreas.get(areaIndex++), start, random);
                    continue;
                }

                ring++;
                if (ring > maxRing) {
                    done = true;
                    return null;
                }
                buildRing();
            }
            return null;
        }

        private void buildRing() {
            currentRingAreas = new ArrayList<>();
            areaIndex = 0;
            currentArea = null;

            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }

                    int chunkX = centerChunkX + dx;
                    int chunkZ = centerChunkZ + dz;
                    if (zonePlan != null && !zonePlan.hasChunk(new ChunkPos(chunkX, chunkZ))) {
                        continue;
                    }

                    ScanArea area = bounds.clipChunk(chunkX, chunkZ);
                    if (area != null) {
                        currentRingAreas.add(area);
                    }
                }
            }

            currentRingAreas.sort((a, b) -> {
                int distance = Double.compare(a.horizontalDistanceSqrTo(start), b.horizontalDistanceSqrTo(start));
                if (distance != 0) {
                    return distance;
                }
                int chunkXCompare = Integer.compare(a.chunkX(), b.chunkX());
                return chunkXCompare != 0 ? chunkXCompare : Integer.compare(a.chunkZ(), b.chunkZ());
            });

            if (random && currentRingAreas.size() > 1) {
                Collections.shuffle(currentRingAreas);
            }
        }

        private static ScanBounds createBounds(Level level, BlockPos start, int range, IZone zone) {
            int levelMinY = level.getMinBuildHeight();
            int levelMaxY = level.getMaxBuildHeight() - 1;

            if (zone instanceof IBox box) {
                BlockPos min = box.min();
                BlockPos max = box.max();
                if (min == null || max == null) {
                    return null;
                }

                BlockPos size = box.size();
                boolean markerLikeExact = size.getX() <= MARKER_ZONE_MAX_EXACT_SIZE
                        && size.getZ() <= MARKER_ZONE_MAX_EXACT_SIZE;

                int rawMinY = Math.min(min.getY(), max.getY());
                int rawMaxY = Math.max(min.getY(), max.getY());
                int minY = Math.max(rawMinY, levelMinY);
                int maxY = Math.min(rawMaxY, levelMaxY);
                if (minY > maxY) {
                    return null;
                }

                // Marker-like boxes (<= 65x65) are scanned exactly and without the default 96 block cap.
                // Bigger boxes still use ring ordering, but every scanned chunk is clipped to the real marker volume.
                return new ScanBounds(
                        Math.min(min.getX(), max.getX()),
                        minY,
                        Math.min(min.getZ(), max.getZ()),
                        Math.max(min.getX(), max.getX()),
                        maxY,
                        Math.max(min.getZ(), max.getZ()),
                        markerLikeExact
                );
            }

            if (zone instanceof ZonePlan plan) {
                if (plan.getChunkPoses().isEmpty()) {
                    return null;
                }

                int minChunkX = Integer.MAX_VALUE;
                int minChunkZ = Integer.MAX_VALUE;
                int maxChunkX = Integer.MIN_VALUE;
                int maxChunkZ = Integer.MIN_VALUE;
                for (ChunkPos chunkPos : plan.getChunkPoses()) {
                    minChunkX = Math.min(minChunkX, chunkPos.x);
                    minChunkZ = Math.min(minChunkZ, chunkPos.z);
                    maxChunkX = Math.max(maxChunkX, chunkPos.x);
                    maxChunkZ = Math.max(maxChunkZ, chunkPos.z);
                }

                // Zone Planner zones are X/Z plans. They use the robot's default vertical search range and no 96 block cap.
                return new ScanBounds(
                        minChunkX << 4,
                        levelMinY,
                        minChunkZ << 4,
                        (maxChunkX << 4) + 15,
                        levelMaxY,
                        (maxChunkZ << 4) + 15,
                        false
                );
            }

            // Unknown IZone implementations do not expose bounds, so keep the old 96 block safety range and filter by contains().
            return new ScanBounds(
                    start.getX() - range,
                    levelMinY,
                    start.getZ() - range,
                    start.getX() + range,
                    levelMaxY,
                    start.getZ() + range,
                    false
            );
        }

    }

    private record ScanBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean markerLikeExact) {
        private boolean isEmpty() {
            return minX > maxX || minY > maxY || minZ > maxZ;
        }

        private int maxRingFrom(int centerChunkX, int centerChunkZ) {
            int minChunkX = minX >> 4;
            int minChunkZ = minZ >> 4;
            int maxChunkX = maxX >> 4;
            int maxChunkZ = maxZ >> 4;
            return Math.max(
                    Math.max(Math.abs(minChunkX - centerChunkX), Math.abs(maxChunkX - centerChunkX)),
                    Math.max(Math.abs(minChunkZ - centerChunkZ), Math.abs(maxChunkZ - centerChunkZ))
            );
        }

        private ScanArea clipChunk(int chunkX, int chunkZ) {
            int chunkMinX = chunkX << 4;
            int chunkMinZ = chunkZ << 4;
            int clippedMinX = Math.max(minX, chunkMinX);
            int clippedMinZ = Math.max(minZ, chunkMinZ);
            int clippedMaxX = Math.min(maxX, chunkMinX + 15);
            int clippedMaxZ = Math.min(maxZ, chunkMinZ + 15);

            if (clippedMinX > clippedMaxX || clippedMinZ > clippedMaxZ) {
                return null;
            }
            return new ScanArea(chunkX, chunkZ, clippedMinX, minY, clippedMinZ, clippedMaxX, maxY, clippedMaxZ);
        }
    }

    private record ScanArea(int chunkX, int chunkZ, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        private double horizontalDistanceSqrTo(BlockPos start) {
            double x = clamp(start.getX(), minX, maxX);
            double z = clamp(start.getZ(), minZ, maxZ);
            double dx = start.getX() - x;
            double dz = start.getZ() - z;
            return dx * dx + dz * dz;
        }

        private static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }
    }

    private static final class AreaIterator {
        private final int[] xs;
        private final int[] ys;
        private final int[] zs;
        private int xIndex;
        private int yIndex;
        private int zIndex;

        private AreaIterator(ScanArea area, BlockPos start, boolean random) {
            this.xs = buildAxisOrder(area.minX(), area.maxX(), start.getX(), random);
            this.ys = buildAxisOrder(area.minY(), area.maxY(), start.getY(), random);
            this.zs = buildAxisOrder(area.minZ(), area.maxZ(), start.getZ(), random);
        }

        private BlockPos next() {
            if (yIndex >= ys.length) {
                return null;
            }

            BlockPos pos = new BlockPos(xs[xIndex], ys[yIndex], zs[zIndex]);
            zIndex++;
            if (zIndex >= zs.length) {
                zIndex = 0;
                xIndex++;
                if (xIndex >= xs.length) {
                    xIndex = 0;
                    yIndex++;
                }
            }
            return pos;
        }

        private static int[] buildAxisOrder(int min, int max, int origin, boolean random) {
            int[] values = new int[max - min + 1];
            if (random) {
                for (int i = 0; i < values.length; i++) {
                    values[i] = min + i;
                }
                shuffle(values);
                return values;
            }

            int index = 0;
            int maxDelta = Math.max(Math.abs(origin - min), Math.abs(origin - max));
            for (int delta = 0; delta <= maxDelta; delta++) {
                int left = origin - delta;
                int right = origin + delta;
                if (left >= min && left <= max) {
                    values[index++] = left;
                }
                if (delta != 0 && right >= min && right <= max) {
                    values[index++] = right;
                }
            }
            return values;
        }

        private static void shuffle(int[] values) {
            for (int i = values.length - 1; i > 0; i--) {
                int j = java.util.concurrent.ThreadLocalRandom.current().nextInt(i + 1);
                int tmp = values[i];
                values[i] = values[j];
                values[j] = tmp;
            }
        }
    }

}
