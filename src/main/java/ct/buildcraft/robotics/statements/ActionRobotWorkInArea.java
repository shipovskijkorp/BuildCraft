package ct.buildcraft.robotics.statements;

import java.util.Random;

import ct.buildcraft.api.core.IBox;
import ct.buildcraft.api.core.IZone;
import ct.buildcraft.api.items.IMapLocation;
import ct.buildcraft.api.statements.IActionInternal;
import ct.buildcraft.api.statements.IStatement;
import ct.buildcraft.api.statements.IStatementContainer;
import ct.buildcraft.api.statements.IStatementParameter;
import ct.buildcraft.api.statements.StatementSlot;
import ct.buildcraft.core.statements.BCStatement;
import ct.buildcraft.lib.client.sprite.SpriteHolderRegistry.SpriteHolder;
import ct.buildcraft.robotics.BCRoboticsSprites;
import ct.buildcraft.robotics.BCRoboticsStatements;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ActionRobotWorkInArea extends BCStatement implements IActionInternal {

    public enum AreaType { WORK, LOAD_UNLOAD }

    public final boolean loadUnload;

    public ActionRobotWorkInArea(boolean loadUnload) {
        super(loadUnload ? "buildcraft:robot.load_unload_area" : "buildcraft:robot.work_in_area");
        this.loadUnload = loadUnload;
    }

    @Override
    public int maxParameters() { return 1; }
    @Override
    public int minParameters() { return 1; }

    @Override
    public IStatementParameter createParameter(int index) {
        return new StatementParameterMapLocation();
    }

    @Override
    public Component getDescription() {
        return Component.translatable(loadUnload ? "gate.action.robot.load_unload_area" : "gate.action.robot.work_in_area");
    }

    @Override
    public void actionActivate(IStatementContainer container, IStatementParameter[] parameters) {
        // The area constraint is read by the robot AI boards
    }

    public static IMapLocation getMapLocation(IStatementParameter[] parameters) {
        if (parameters == null || parameters.length == 0) return null;
        IStatementParameter p = parameters[0];
        if (!(p instanceof StatementParameterMapLocation spl)) return null;
        ItemStack stack = spl.getItemStack();
        if (stack.isEmpty() || !(stack.getItem() instanceof IMapLocation)) return null;
        return (IMapLocation) stack.getItem();
    }

    public static IZone getArea(StatementSlot slot) {
        if (slot == null || slot.parameters == null || slot.parameters.length == 0) {
            return null;
        }
        IStatementParameter parameter = slot.parameters[0];
        if (!(parameter instanceof StatementParameterMapLocation mapParam)) {
            return null;
        }
        ItemStack stack = mapParam.getItemStack();
        if (stack.isEmpty() || !(stack.getItem() instanceof IMapLocation map)) {
            return null;
        }

        if (IMapLocation.MapLocationType.getFromStack(stack) == IMapLocation.MapLocationType.AREA) {
            IBox box = map.getBox(stack);
            return box == null ? null : new HorizontalAreaZone(box);
        }

        return map.getZone(stack);
    }

    /**
     * Robot work areas are treated as a horizontal map area. Volume markers still store their full 3D
     * box in the Map Location item, but the robot action should use only the X/Z footprint. This matches
     * the way players use a marker area to fence off a working plot: trees, dropped items, and stations
     * may be above or below the marker's Y range and should still count as inside the area.
     */
    private static final class HorizontalAreaZone implements IZone {
        private final IBox area;

        private HorizontalAreaZone(IBox area) {
            this.area = area;
        }

        @Override
        public double distanceTo(BlockPos pos) {
            return Math.sqrt(distanceToSquared(pos));
        }

        @Override
        public double distanceToSquared(BlockPos pos) {
            double dx = axisDistance(pos.getX(), area.min().getX(), area.max().getX());
            double dz = axisDistance(pos.getZ(), area.min().getZ(), area.max().getZ());
            return dx * dx + dz * dz;
        }

        @Override
        public boolean contains(Vec3 point) {
            int x = (int) Math.floor(point.x);
            int z = (int) Math.floor(point.z);
            return x >= area.min().getX() && x <= area.max().getX()
                    && z >= area.min().getZ() && z <= area.max().getZ();
        }

        @Override
        public BlockPos getRandomBlockPos(Random rand) {
            int x = randomInclusive(rand, area.min().getX(), area.max().getX());
            int z = randomInclusive(rand, area.min().getZ(), area.max().getZ());
            return new BlockPos(x, area.min().getY(), z);
        }

        private static double axisDistance(int value, int min, int max) {
            if (value < min) {
                return min - value;
            }
            if (value > max) {
                return value - max;
            }
            return 0;
        }

        private static int randomInclusive(Random rand, int min, int max) {
            return min + rand.nextInt(max - min + 1);
        }
    }

    public AreaType getAreaType() {
        return loadUnload ? AreaType.LOAD_UNLOAD : AreaType.WORK;
    }

    @Override
    public IStatement[] getPossible() {
        return new IStatement[] {
            BCRoboticsStatements.ACTION_ROBOT_WORK_IN_AREA,
            BCRoboticsStatements.ACTION_ROBOT_LOAD_UNLOAD_AREA
        };
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public SpriteHolder getSprite() {
        return loadUnload ? BCRoboticsSprites.ACTION_ROBOT_LOAD_UNLOAD_AREA : BCRoboticsSprites.ACTION_ROBOT_WORK_IN_AREA;
    }
}
