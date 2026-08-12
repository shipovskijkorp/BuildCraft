package buildcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.transport.internal.pipe.IFlowItems;
import buildcraft.transport.internal.pipe.PipeEventHandler;
import buildcraft.transport.internal.pipe.PipeEventItem;
import buildcraft.lib.BCLib;
import buildcraft.transport.BCTransportPipes;
import buildcraft.transport.pipe.behaviour.PipeBehaviourDiamond;
import buildcraft.transport.pipe.behaviour.PipeBehaviourDiamondItem;
import buildcraft.transport.pipe.flow.PipeFlowItems;
import buildcraft.transport.tile.TilePipeHolder;

@GameTestHolder(BCLib.MODID)
@PrefixGameTestTemplate(false)
public final class BuildCraftPipeTransportGameTests {
    private BuildCraftPipeTransportGameTests() {
    }

    @GameTest(templateNamespace = BCLib.MODID, template = PipeGameTestSupport.LARGE_EMPTY_TEMPLATE,
        timeoutTicks = 360)
    public static void straightItemLineConservesTaggedCargo(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(1, 1, 3);
        BlockPos firstPipePos = new BlockPos(2, 1, 3);
        BlockPos secondPipePos = new BlockPos(3, 1, 3);
        BlockPos destinationPos = new BlockPos(4, 1, 3);

        placeChest(helper, sourcePos);
        ChestBlockEntity destination = placeChest(helper, destinationPos);
        TilePipeHolder first = PipeGameTestSupport.placePipe(helper, firstPipePos, BCTransportPipes.cobbleItem);
        TilePipeHolder second = PipeGameTestSupport.placePipe(helper, secondPipePos, BCTransportPipes.cobbleItem);

        ItemStack cargo = new ItemStack(Items.APPLE, 12);
        cargo.setHoverName(Component.literal("tagged pipe cargo"));
        cargo.getOrCreateTag().putString("buildcraft_test", "straight_line");

        helper.runAfterDelay(5, () -> {
            require(helper, first.getPipe().isConnected(Direction.WEST), "first pipe did not connect to source chest");
            require(helper, first.getPipe().isConnected(Direction.EAST), "first pipe did not connect to second pipe");
            require(helper, second.getPipe().isConnected(Direction.WEST), "second pipe did not connect to first pipe");
            require(helper, second.getPipe().isConnected(Direction.EAST), "second pipe did not connect to destination");

            IFlowItems flow = (IFlowItems) first.getPipe().getFlow();
            ItemStack leftover = flow.injectItem(cargo.copy(), true, Direction.WEST, null, 0.05);
            require(helper, leftover.isEmpty(), "straight line rejected " + leftover.getCount() + " cargo items");
        });

        helper.runAfterDelay(280, () -> {
            ItemStack delivered = firstNonEmpty(destination);
            require(helper, delivered.is(Items.APPLE), "destination received the wrong item: " + delivered);
            require(helper, delivered.getCount() == 12, "destination received " + delivered.getCount() + " of 12 items");
            require(helper, delivered.hasCustomHoverName()
                && delivered.getHoverName().getString().equals("tagged pipe cargo"),
                "custom name was lost in transit");
            require(helper, delivered.hasTag()
                && "straight_line".equals(delivered.getTag().getString("buildcraft_test")),
                "custom NBT was lost in transit");
            require(helper, !((PipeFlowItems) first.getPipe().getFlow()).doesContainItems(),
                "first pipe still contained cargo after delivery");
            require(helper, !((PipeFlowItems) second.getPipe().getFlow()).doesContainItems(),
                "second pipe still contained cargo after delivery");
            require(helper, countDroppedItems(helper) == 0, "straight route dropped an ItemEntity");
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = BCLib.MODID, template = PipeGameTestSupport.LARGE_EMPTY_TEMPLATE,
        timeoutTicks = 300)
    public static void diamondFullPreferredDestinationFallsBackWithoutLoss(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(2, 1, 3);
        BlockPos pipePos = new BlockPos(3, 1, 3);
        BlockPos fullChestPos = new BlockPos(4, 1, 3);
        BlockPos fallbackChestPos = new BlockPos(3, 1, 4);

        placeChest(helper, sourcePos);
        ChestBlockEntity fullChest = placeChest(helper, fullChestPos);
        ChestBlockEntity fallbackChest = placeChest(helper, fallbackChestPos);
        fillCompletely(fullChest);
        TilePipeHolder holder = PipeGameTestSupport.placePipe(helper, pipePos, BCTransportPipes.diamondItem);

        PipeBehaviourDiamondItem behaviour = (PipeBehaviourDiamondItem) holder.getPipe().getBehaviour();
        behaviour.filters.setStackInSlot(
            Direction.EAST.ordinal() * PipeBehaviourDiamond.FILTERS_PER_SIDE,
            new ItemStack(Items.APPLE)
        );

        helper.runAfterDelay(5, () -> {
            require(helper, holder.getPipe().isConnected(Direction.WEST), "diamond pipe did not connect to input");
            require(helper, holder.getPipe().isConnected(Direction.EAST), "diamond pipe did not connect to full chest");
            require(helper, holder.getPipe().isConnected(Direction.SOUTH), "diamond pipe did not connect to fallback chest");

            ItemStack input = new ItemStack(Items.APPLE, 13);
            ItemStack leftover = ((IFlowItems) holder.getPipe().getFlow())
                .injectItem(input, true, Direction.WEST, null, 0.05);
            require(helper, leftover.isEmpty(), "diamond pipe rejected initial input");
        });

        helper.runAfterDelay(220, () -> {
            require(helper, PipeGameTestSupport.countItem(fullChest, Items.APPLE) == 0,
                "full preferred chest received apples despite having no space");
            require(helper, PipeGameTestSupport.countItem(fallbackChest, Items.APPLE) == 13,
                "fallback route did not preserve all 13 items");
            require(helper, !((PipeFlowItems) holder.getPipe().getFlow()).doesContainItems(),
                "diamond pipe kept retrying a rejected destination");
            require(helper, countDroppedItems(helper) == 0, "diamond fallback dropped cargo");
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = BCLib.MODID, template = PipeGameTestSupport.LARGE_EMPTY_TEMPLATE,
        timeoutTicks = 180)
    public static void itemInjectionSimulationAndAcceptedCountAreConsistent(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(2, 1, 3);
        BlockPos pipePos = new BlockPos(3, 1, 3);
        BlockPos destinationPos = new BlockPos(4, 1, 3);

        placeChest(helper, sourcePos);
        ChestBlockEntity destination = placeChest(helper, destinationPos);
        TilePipeHolder holder = PipeGameTestSupport.placePipe(helper, pipePos, BCTransportPipes.goldItem);
        holder.eventBus.registerHandler(new ClampInsertHandler(3));

        helper.runAfterDelay(5, () -> {
            PipeFlowItems flow = (PipeFlowItems) holder.getPipe().getFlow();
            ItemStack input = new ItemStack(Items.APPLE, 10);

            ItemStack simulatedLeftover = flow.injectItem(input, false, Direction.WEST, null, 0.05);
            require(helper, input.getCount() == 10, "simulated insertion mutated its input stack");
            require(helper, simulatedLeftover.getCount() == 7,
                "simulated insertion did not respect accepted=3");
            require(helper, !flow.doesContainItems(), "simulated insertion created travelling cargo");

            ItemStack executedLeftover = flow.injectItem(input, true, Direction.WEST, null, 0.05);
            require(helper, input.getCount() == 10, "executed insertion mutated its input stack");
            require(helper, executedLeftover.getCount() == 7,
                "executed insertion did not return the same remainder as simulation");
        });

        helper.runAfterDelay(100, () -> {
            require(helper, PipeGameTestSupport.countItem(destination, Items.APPLE) == 3,
                "accepted-count clamp delivered the wrong number of items");
            require(helper, !((PipeFlowItems) holder.getPipe().getFlow()).doesContainItems(),
                "accepted cargo remained stuck in the pipe");
            require(helper, countDroppedItems(helper) == 0, "accepted-count test dropped cargo");
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = BCLib.MODID, template = PipeGameTestSupport.LARGE_EMPTY_TEMPLATE,
        timeoutTicks = 320)
    public static void clayPipePrefersInventoryOverAnotherValidPipe(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(2, 1, 3);
        BlockPos clayPos = new BlockPos(3, 1, 3);
        BlockPos preferredChestPos = new BlockPos(4, 1, 3);
        BlockPos alternatePipePos = new BlockPos(3, 1, 4);
        BlockPos alternateChestPos = new BlockPos(3, 1, 5);

        placeChest(helper, sourcePos);
        ChestBlockEntity preferred = placeChest(helper, preferredChestPos);
        ChestBlockEntity alternate = placeChest(helper, alternateChestPos);
        TilePipeHolder clay = PipeGameTestSupport.placePipe(helper, clayPos, BCTransportPipes.clayItem);
        TilePipeHolder alternatePipe = PipeGameTestSupport.placePipe(helper, alternatePipePos, BCTransportPipes.cobbleItem);

        helper.runAfterDelay(5, () -> {
            require(helper, clay.getPipe().isConnected(Direction.EAST), "clay pipe did not connect to inventory");
            require(helper, clay.getPipe().isConnected(Direction.SOUTH), "clay pipe did not connect to alternate pipe");

            ItemStack leftover = ((IFlowItems) clay.getPipe().getFlow())
                .injectItem(new ItemStack(Items.IRON_INGOT, 8), true, Direction.WEST, null, 0.05);
            require(helper, leftover.isEmpty(), "clay pipe rejected initial input");
        });

        helper.runAfterDelay(240, () -> {
            require(helper, PipeGameTestSupport.countItem(preferred, Items.IRON_INGOT) == 8,
                "clay pipe did not prioritise the adjacent inventory");
            require(helper, PipeGameTestSupport.countItem(alternate, Items.IRON_INGOT) == 0,
                "clay pipe sent cargo into the lower-priority pipe route");
            require(helper, !((PipeFlowItems) clay.getPipe().getFlow()).doesContainItems(),
                "clay pipe still contained cargo after delivery");
            require(helper, !((PipeFlowItems) alternatePipe.getPipe().getFlow()).doesContainItems(),
                "alternate pipe unexpectedly received cargo");
            require(helper, countDroppedItems(helper) == 0, "clay route dropped cargo");
            helper.succeed();
        });
    }

    private static ChestBlockEntity placeChest(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.CHEST.defaultBlockState());
        BlockEntity blockEntity = helper.getBlockEntity(pos);
        if (!(blockEntity instanceof ChestBlockEntity chest)) {
            helper.fail("chest block did not create ChestBlockEntity at " + pos);
            throw new IllegalStateException("missing chest");
        }
        return chest;
    }

    private static void fillCompletely(ChestBlockEntity chest) {
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            chest.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
    }

    private static ItemStack firstNonEmpty(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static int countDroppedItems(GameTestHelper helper) {
        BlockPos min = helper.absolutePos(BlockPos.ZERO);
        BlockPos max = helper.absolutePos(new BlockPos(7, 3, 7));
        AABB bounds = new AABB(
            min.getX(), min.getY(), min.getZ(),
            max.getX(), max.getY(), max.getZ()
        );
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, bounds).size();
    }

    public static final class ClampInsertHandler {
        private final int maximum;

        public ClampInsertHandler(int maximum) {
            this.maximum = maximum;
        }

        @PipeEventHandler
        public void clamp(PipeEventItem.TryInsert event) {
            event.accepted = Math.min(event.accepted, maximum);
        }
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
