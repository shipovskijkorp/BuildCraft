package ct.buildcraft.robotics.ai;

import ct.buildcraft.api.core.IZone;
import ct.buildcraft.api.robots.AIRobot;
import ct.buildcraft.api.robots.EntityRobotBase;
import ct.buildcraft.robotics.boards.BoardRobotPicker;
import ct.buildcraft.robotics.entity.EntityRobot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AIRobotFetchItem extends AIRobot {
    private ItemEntity target;
    private int targetId = -1;
    private float maxRange = 250;
    private int pickTime = -1;
    private IZone zone;

    public AIRobotFetchItem(EntityRobotBase robot) {
        super(robot);
    }

    public AIRobotFetchItem(EntityRobotBase robot, float maxRange, IZone zone) {
        this(robot);
        this.maxRange = maxRange;
        this.zone = zone;
    }

    @Override
    public void preempt(AIRobot ai) {
        if (target != null && (!target.isAlive() || target.getItem().isEmpty())) {
            terminate();
        }
    }

    @Override
    public void update() {
        if (target == null && targetId != -1) {
            AABB lookup = robot.getBoundingBox().inflate(maxRange);
            for (ItemEntity item : robot.level.getEntitiesOfClass(ItemEntity.class, lookup)) {
                if (item.getId() == targetId) {
                    target = item;
                    break;
                }
            }
        }
        if (target == null) {
            scanForItem();
        } else {
            if (Math.floor(target.getX()) != Math.floor(robot.getX())
                    || Math.floor(target.getY()) != Math.floor(robot.getY())
                    || Math.floor(target.getZ()) != Math.floor(robot.getZ())) {
                startDelegateAI(new AIRobotGotoBlock(robot, (int) Math.floor(target.getX()), (int) Math.floor(target.getY()), (int) Math.floor(target.getZ())));
                return;
            }
            pickTime++;
            if (pickTime > 5) {
                ItemStack stack = target.getItem();
                ItemStack remaining = insert(stack, true);
                target.setItem(remaining);
                if (remaining.isEmpty()) {
                    target.discard();
                }
                terminate();
            }
        }
    }

    @Override
    public void delegateAIEnded(AIRobot ai) {
        if (ai instanceof AIRobotGotoBlock) {
            if (target == null || !target.isAlive() || target.getItem().isEmpty()) {
                setSuccess(false);
                terminate();
            } else if (!ai.success()) {
                robot.unreachableEntityDetected(target);
                setSuccess(false);
                terminate();
            } else {
                pickTime = 0;
            }
        }
    }

    @Override
    public void end() {
        if (targetId != -1) {
            BoardRobotPicker.targettedItems.remove(targetId);
        }
    }

    private void scanForItem() {
        double bestDistance = Double.MAX_VALUE;
        ItemEntity best = null;
        AABB box = robot.getBoundingBox().inflate(maxRange);
        for (ItemEntity item : robot.level.getEntitiesOfClass(ItemEntity.class, box)) {
            if (!item.isAlive() || item.getItem().isEmpty()) continue;
            if (BoardRobotPicker.targettedItems.contains(item.getId())) continue;
            if (robot.isKnownUnreachable(item)) continue;
            if (zone != null && !zone.contains(new Vec3(item.getX(), item.getY(), item.getZ()))) continue;
            double sqrDistance = item.distanceToSqr(robot);
            if (sqrDistance >= maxRange * maxRange) continue;
            if (insert(item.getItem(), false).getCount() >= item.getItem().getCount()) continue;
            if (best == null || sqrDistance < bestDistance) {
                best = item;
                bestDistance = sqrDistance;
            }
        }
        if (best != null) {
            target = best;
            targetId = best.getId();
            BoardRobotPicker.targettedItems.add(targetId);
            if (Math.floor(target.getX()) != Math.floor(robot.getX())
                    || Math.floor(target.getY()) != Math.floor(robot.getY())
                    || Math.floor(target.getZ()) != Math.floor(robot.getZ())) {
                startDelegateAI(new AIRobotGotoBlock(robot, (int) Math.floor(target.getX()), (int) Math.floor(target.getY()), (int) Math.floor(target.getZ())));
            }
        } else {
            setSuccess(false);
            terminate();
        }
    }

    private ItemStack insert(ItemStack stack, boolean doInsert) {
        if (robot instanceof EntityRobot entityRobot) {
            return entityRobot.insertIntoInventory(stack, doInsert);
        }
        return stack;
    }

    @Override
    public int getEnergyCost() {
        return 15;
    }

    @Override
    public boolean canLoadFromNBT() {
        return true;
    }

    @Override
    public void writeSelfToNBT(CompoundTag nbt) {
        nbt.putFloat("maxRange", maxRange);
        nbt.putInt("targetId", targetId);
        nbt.putInt("pickTime", pickTime);
    }

    @Override
    public void loadSelfFromNBT(CompoundTag nbt) {
        maxRange = nbt.contains("maxRange") ? nbt.getFloat("maxRange") : 250;
        targetId = nbt.getInt("targetId");
        pickTime = nbt.getInt("pickTime");
    }
}
