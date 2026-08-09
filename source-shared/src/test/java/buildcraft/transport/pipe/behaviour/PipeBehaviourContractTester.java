package buildcraft.transport.pipe.behaviour;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import buildcraft.api.transport.pipe.PipeBehaviour;
import buildcraft.api.transport.pipe.PipeEventItem;
import buildcraft.transport.pipe.Pipe;

/** Fast, deterministic guards for pipe contracts inherited from BuildCraft 8. */
public class PipeBehaviourContractTester {
    private static final double EPSILON = 1.0e-12;

    @Test
    void itemPipeSpeedProfilesStayCompatibleWithBuildCraft8() {
        assertSpeed(PipeBehaviourGold::modifySpeed, 0.25, 0.07);
        assertSpeed(PipeBehaviourStone::modifySpeed, 0.01, 0.008);
        assertSpeed(PipeBehaviourCobble::modifySpeed, 0.01, 0.02);
        assertSpeed(PipeBehaviourQuartz::modifySpeed, 0.01, 0.002);
        assertSpeed(PipeBehaviourSandstone::modifySpeed, 0.01, 0.008);
    }

    @Test
    void separatePipeFamiliesOnlyConnectToTheirOwnMaterial() {
        PipeBehaviour stone = new PipeBehaviourStone(null);
        PipeBehaviour cobble = new PipeBehaviourCobble(null);
        PipeBehaviour quartz = new PipeBehaviourQuartz(null);
        PipeBehaviour gold = new PipeBehaviourGold(null);

        Assertions.assertTrue(stone.canConnect(Direction.EAST, new PipeBehaviourStone(null)));
        Assertions.assertTrue(cobble.canConnect(Direction.EAST, new PipeBehaviourCobble(null)));
        Assertions.assertTrue(quartz.canConnect(Direction.EAST, new PipeBehaviourQuartz(null)));

        assertDisconnectedBothWays(stone, cobble);
        assertDisconnectedBothWays(stone, quartz);
        assertDisconnectedBothWays(cobble, quartz);

        Assertions.assertTrue(stone.canConnect(Direction.EAST, gold));
        Assertions.assertTrue(gold.canConnect(Direction.WEST, stone));
    }

    @Test
    void sandstoneConnectsToPipesButNeverDirectlyToTiles() {
        PipeBehaviourSandstone sandstone = new PipeBehaviourSandstone(null);
        Assertions.assertTrue(sandstone.canConnect(Direction.EAST, new PipeBehaviourGold(null)));
        Assertions.assertFalse(sandstone.canConnect(Direction.EAST, (BlockEntity) null));
    }

    private static void assertDisconnectedBothWays(PipeBehaviour first, PipeBehaviour second) {
        Assertions.assertFalse(first.canConnect(Direction.EAST, second));
        Assertions.assertFalse(second.canConnect(Direction.WEST, first));
    }

    private static void assertSpeed(SpeedModifier modifier, double target, double delta) {
        PipeEventItem.ModifySpeed event = new PipeEventItem.ModifySpeed(null, null, null, 0.05);
        modifier.modify(event);
        Assertions.assertEquals(target, event.targetSpeed, EPSILON);
        Assertions.assertEquals(delta, event.maxSpeedChange, EPSILON);
    }

    @FunctionalInterface
    private interface SpeedModifier {
        void modify(PipeEventItem.ModifySpeed event);
    }
}
