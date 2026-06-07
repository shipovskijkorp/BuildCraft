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
import ct.buildcraft.api.robots.EntityRobotBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Pathing port for picker-level movement. It follows the old robot rule of travelling through soft/non-colliding blocks. */
public class AIRobotGotoBlock extends AIRobotGoto {
    private LinkedList<BlockIndex> path;
    private double prevDistance = Double.MAX_VALUE;
    private int finalX, finalY, finalZ;
    private double maxDistance;
    private BlockIndex lastBlockInPath;
    private boolean loadedFromNBT;

    public AIRobotGotoBlock(EntityRobotBase robot) {
        super(robot);
    }

    public AIRobotGotoBlock(EntityRobotBase robot, int x, int y, int z) {
        this(robot, x, y, z, 0);
    }

    public AIRobotGotoBlock(EntityRobotBase robot, int x, int y, int z, double maxDistance) {
        super(robot);
        this.finalX = x;
        this.finalY = y;
        this.finalZ = z;
        this.maxDistance = maxDistance;
    }

    public AIRobotGotoBlock(EntityRobotBase robot, LinkedList<BlockIndex> path) {
        super(robot);
        this.path = path;
        if (!path.isEmpty()) {
            BlockIndex last = path.getLast();
            finalX = last.x;
            finalY = last.y;
            finalZ = last.z;
            setNextInPath();
        }
    }

    @Override
    public void start() {
        robot.undock();
    }

    @Override
    public void update() {
        if (loadedFromNBT) {
            setNextInPath();
            loadedFromNBT = false;
        }
        if (path == null) {
            path = findPath();
            if (path == null || path.isEmpty()) {
                setSuccess(false);
                terminate();
                return;
            }
            lastBlockInPath = path.getLast();
            setNextInPath();
        }

        if (path != null && !path.isEmpty()) {
            double distance = distanceTo(nextX, nextY, nextZ);
            if (distance <= 0.18D || !robot.isMoving() || distance > prevDistance + 0.025D) {
                path.removeFirst();
                setNextInPath();
            } else {
                prevDistance = distance;
            }
        }

        if (path != null && path.isEmpty()) {
            robot.setDeltaMovement(Vec3.ZERO);
            if (lastBlockInPath != null) {
                robot.setPos(lastBlockInPath.x + 0.5D, lastBlockInPath.y + 0.5D, lastBlockInPath.z + 0.5D);
            }
            terminate();
        }
    }

    private double distanceTo(double x, double y, double z) {
        double dx = robot.getX() - x;
        double dy = robot.getY() - y;
        double dz = robot.getZ() - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private void setNextInPath() {
        if (path != null && !path.isEmpty()) {
            BlockIndex next = path.getFirst();
            prevDistance = Double.MAX_VALUE;
            setDestination(robot, next.x + 0.5D, next.y + 0.5D, next.z + 0.5D);
            robot.aimItemAt(next.x, next.y, next.z);
        }
    }

    private LinkedList<BlockIndex> findPath() {
        BlockPos start = robot.blockPosition();
        BlockPos target = new BlockPos(finalX, finalY, finalZ);
        if (start.equals(target)) {
            LinkedList<BlockIndex> single = new LinkedList<>();
            single.add(new BlockIndex(target));
            return single;
        }
        Level level = robot.level;
        int hardLimit = maxDistance > 0 ? Math.max(1, (int) Math.ceil(maxDistance)) : 96;
        int maxVisited = 96 * 96;
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        Map<BlockPos, BlockPos> parent = new HashMap<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && seen.size() < maxVisited) {
            BlockPos cur = queue.remove();
            for (Direction dir : Direction.values()) {
                BlockPos next = cur.relative(dir);
                if (seen.contains(next)) continue;
                if (maxDistance > 0 && distanceSqr(next, start) > hardLimit * hardLimit) continue;
                if (!next.equals(target) && !isSoft(level, next)) continue;
                parent.put(next, cur);
                if (next.equals(target)) {
                    return reconstruct(start, target, parent);
                }
                seen.add(next);
                queue.add(next);
            }
        }
        return directFallback(start, target, level);
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

    private static LinkedList<BlockIndex> reconstruct(BlockPos start, BlockPos target, Map<BlockPos, BlockPos> parent) {
        List<BlockPos> reversed = new ArrayList<>();
        BlockPos cur = target;
        while (cur != null && !cur.equals(start)) {
            reversed.add(cur);
            cur = parent.get(cur);
        }
        Collections.reverse(reversed);
        LinkedList<BlockIndex> out = new LinkedList<>();
        for (BlockPos pos : reversed) out.add(new BlockIndex(pos));
        return out;
    }

    private static LinkedList<BlockIndex> directFallback(BlockPos start, BlockPos target, Level level) {
        LinkedList<BlockIndex> out = new LinkedList<>();
        int x = start.getX();
        int y = start.getY();
        int z = start.getZ();
        while (x != target.getX() || y != target.getY() || z != target.getZ()) {
            if (x < target.getX()) x++; else if (x > target.getX()) x--;
            else if (y < target.getY()) y++; else if (y > target.getY()) y--;
            else if (z < target.getZ()) z++; else if (z > target.getZ()) z--;
            BlockPos pos = new BlockPos(x, y, z);
            if (!pos.equals(target) && !isSoft(level, pos)) return null;
            out.add(new BlockIndex(pos));
        }
        return out;
    }

    @Override
    public void end() {
        robot.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public boolean canLoadFromNBT() {
        return true;
    }

    @Override
    public void writeSelfToNBT(CompoundTag nbt) {
        nbt.putInt("finalX", finalX);
        nbt.putInt("finalY", finalY);
        nbt.putInt("finalZ", finalZ);
        nbt.putDouble("maxDistance", maxDistance);
    }

    @Override
    public void loadSelfFromNBT(CompoundTag nbt) {
        finalX = nbt.getInt("finalX");
        finalY = nbt.getInt("finalY");
        finalZ = nbt.getInt("finalZ");
        maxDistance = nbt.getDouble("maxDistance");
        loadedFromNBT = true;
    }
}
