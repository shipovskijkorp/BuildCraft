package buildcraft.gametest;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.netty.buffer.Unpooled;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import buildcraft.builders.internal.filler.legacy.FillerManager;
import buildcraft.builders.internal.filler.legacy.IFilledTemplate;
import buildcraft.builders.internal.filler.legacy.IFillerPatternShape;
import buildcraft.api.inventory.IItemTransactor;
import buildcraft.api.v2.item.ItemTransferResult;
import buildcraft.api.v2.reload.DefinitionProvenance;
import buildcraft.api.v2.list.ListMatchType;
import buildcraft.lib.internal.statement.IStatementParameter;
import buildcraft.builders.BCBuildersStatements;
import buildcraft.builders.registry.FillerRegistry;
import buildcraft.builders.snapshot.Snapshot;
import buildcraft.builders.snapshot.Template;
import buildcraft.builders.snapshot.pattern.parameter.PatternParameterFacing;
import buildcraft.builders.snapshot.pattern.parameter.PatternParameterHollow;
import buildcraft.lib.BCLib;
import buildcraft.lib.internal.api.v2.FacadeRuleRegistryImpl;
import buildcraft.lib.fluid.Tank;
import buildcraft.lib.fluid.TankManager;
import buildcraft.lib.list.ListMatchHandlerTools;
import buildcraft.lib.misc.VecUtil;
import buildcraft.lib.net.PacketBufferBC;
import buildcraft.lib.tile.item.ItemHandlerSimple;
import buildcraft.lib.tile.item.StackInsertionFunction;

/**
 * Contract tests that touch vanilla registries, ItemStack, fluids, tags or
 * BuildCraft registrations. They intentionally run as Forge GameTests instead
 * of plain JUnit because Forge 1.19.2 patches Bootstrap.bootStrap() to initialize
 * networking, which is not safe in an unlaunched JUnit JVM.
 */
@GameTestHolder(BCLib.MODID)
@PrefixGameTestTemplate(false)
public final class BuildCraftLogicGameTests {
    private static final String EMPTY_TEMPLATE = "empty3x3x3";

    private static final BlockPos[] SHAPE_SIZES = {
        new BlockPos(1, 1, 1), new BlockPos(2, 1, 1), new BlockPos(3, 1, 1),
        new BlockPos(2, 2, 2), new BlockPos(3, 2, 2), new BlockPos(4, 2, 2),
        new BlockPos(2, 3, 2), new BlockPos(2, 2, 3), new BlockPos(2, 8, 2),
        new BlockPos(3, 3, 3), new BlockPos(4, 4, 4), new BlockPos(5, 5, 5),
        new BlockPos(6, 6, 6), new BlockPos(7, 7, 7), new BlockPos(11, 13, 12)
    };

    private BuildCraftLogicGameTests() {
    }

    @GameTest(templateNamespace = BCLib.MODID, template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void everyShapeHandlesSmallAndNonCubicTemplates(GameTestHelper helper) {
        FillerManager.registry = FillerRegistry.INSTANCE;
        List<IFillerPatternShape> patterns = Arrays.stream(BCBuildersStatements.PATTERNS)
            .filter(IFillerPatternShape.class::isInstance)
            .map(IFillerPatternShape.class::cast)
            .collect(Collectors.toList());

        for (IFillerPatternShape pattern : patterns) {
            for (BlockPos size : SHAPE_SIZES) {
                IStatementParameter[] parameters = defaultParameters(pattern);
                IFilledTemplate template = createFilledTemplate(size);
                boolean filled = pattern.fillTemplate(template, parameters);
                if (pattern == BCBuildersStatements.PATTERN_NONE) {
                    require(helper, !filled, pattern.getUniqueTag() + " unexpectedly filled " + size);
                } else {
                    require(helper, filled, pattern.getUniqueTag() + " rejected " + size);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = BCLib.MODID, template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void hollowHemispheresMatchTheCorrespondingHalfOfAFullSphere(GameTestHelper helper) {
        FillerManager.registry = FillerRegistry.INSTANCE;
        for (BlockPos halfSize : SHAPE_SIZES) {
            BlockPos fullSize = new BlockPos(halfSize.getX() * 2, halfSize.getY() * 2, halfSize.getZ() * 2);
            IFilledTemplate full = createFilledTemplate(fullSize);
            require(helper, BCBuildersStatements.PATTERN_SPHERE.fillTemplate(
                full,
                new IStatementParameter[] { PatternParameterHollow.HOLLOW }
            ), "sphere rejected full size " + fullSize);

            for (Direction face : Direction.values()) {
                BlockPos size = VecUtil.replaceValue(
                    fullSize,
                    face.getAxis(),
                    VecUtil.getValue(halfSize, face.getAxis())
                );
                IFilledTemplate half = createFilledTemplate(size);
                require(helper, BCBuildersStatements.PATTERN_HEMI_SPHERE.fillTemplate(
                    half,
                    new IStatementParameter[] {
                        PatternParameterHollow.HOLLOW,
                        PatternParameterFacing.get(face)
                    }
                ), "hemisphere " + face + " rejected " + size);

                int dx = face == Direction.WEST ? half.getSize().getX() : 0;
                int dy = face == Direction.DOWN ? half.getSize().getY() : 0;
                int dz = face == Direction.NORTH ? half.getSize().getZ() : 0;
                for (int z = 0; z <= half.getMax().getZ(); z++) {
                    for (int y = 0; y <= half.getMax().getY(); y++) {
                        for (int x = 0; x <= half.getMax().getX(); x++) {
                            require(
                                helper,
                                full.get(x + dx, y + dy, z + dz) == half.get(x, y, z),
                                "hemisphere " + face + " mismatch at " + new BlockPos(x, y, z)
                                    + " for full size " + fullSize
                            );
                        }
                    }
                }
            }
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = BCLib.MODID, template = EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void tankManagerFillSpillsAndDrainCombines(GameTestHelper helper) {
        Tank first = new Tank("first", 3_000, null);
        Tank second = new Tank("second", 3_000, null);
        TankManager manager = new TankManager(first, second);

        require(helper, manager.fill(new FluidStack(Fluids.WATER, 5_000), FluidAction.EXECUTE) == 5_000,
            "manager did not accept 5000 mB");
        require(helper, first.getFluidAmount() == 3_000, "first tank did not fill to capacity");
        require(helper, second.getFluidAmount() == 2_000, "second tank did not receive overflow");

        FluidStack simulated = manager.drain(new FluidStack(Fluids.WATER, 4_000), FluidAction.SIMULATE);
        require(helper, simulated.getAmount() == 4_000, "simulated drain returned the wrong amount");
        require(helper, first.getFluidAmount() + second.getFluidAmount() == 5_000,
            "simulated drain mutated stored fluid");

        FluidStack drained = manager.drain(new FluidStack(Fluids.WATER, 4_000), FluidAction.EXECUTE);
        require(helper, drained.getAmount() == 4_000, "executed drain returned the wrong amount");
        require(helper, first.getFluidAmount() + second.getFluidAmount() == 1_000,
            "executed drain left the wrong amount");
        helper.succeed();
    }

    @GameTest(templateNamespace = BCLib.MODID, template = EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void tankManagerSimulationAndNbtRoundTrip(GameTestHelper helper) {
        TankManager original = new TankManager(
            new Tank("input", 1_000, null),
            new Tank("output", 2_000, null)
        );

        require(helper, original.fill(new FluidStack(Fluids.LAVA, 2_500), FluidAction.SIMULATE) == 2_500,
            "simulated fill returned the wrong amount");
        require(helper, original.get(0).getFluidAmount() == 0 && original.get(1).getFluidAmount() == 0,
            "simulated fill mutated tanks");
        require(helper, original.fill(new FluidStack(Fluids.LAVA, 2_500), FluidAction.EXECUTE) == 2_500,
            "executed fill returned the wrong amount");

        CompoundTag nbt = original.serializeNBT(helper.getLevel().registryAccess());
        TankManager restored = new TankManager(
            new Tank("input", 1_000, null),
            new Tank("output", 2_000, null)
        );
        restored.deserializeNBT(helper.getLevel().registryAccess(), nbt);

        require(helper, restored.get(0).getFluidAmount() == 1_000, "first restored tank has wrong amount");
        require(helper, restored.get(1).getFluidAmount() == 1_500, "second restored tank has wrong amount");
        require(helper, restored.get(0).getFluid().getFluid() == Fluids.LAVA, "first restored tank has wrong fluid");
        require(helper, restored.get(1).getFluid().getFluid() == Fluids.LAVA, "second restored tank has wrong fluid");
        helper.succeed();
    }

    @GameTest(templateNamespace = BCLib.MODID, template = EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void tankManagerForgeViewReportsContentsAndCapacity(GameTestHelper helper) {
        TankManager manager = new TankManager(new Tank("only", 4_000, null));
        manager.fill(new FluidStack(Fluids.WATER, 750), FluidAction.EXECUTE);

        require(helper, manager.getTanks() == 1, "manager reported wrong tank count");
        require(helper, manager.getTankCapacity(0) == 4_000, "manager reported wrong tank capacity");
        require(helper, manager.getFluidInTank(0).getAmount() == 750, "manager reported wrong fluid amount");
        require(helper, manager.getFluidInTank(0).getFluid() == Fluids.WATER, "manager reported wrong fluid");
        helper.succeed();
    }

    @GameTest(templateNamespace = BCLib.MODID, template = EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void itemTransactorInsertExtractAndSimulation(GameTestHelper helper) {
        IItemTransactor transactor = new ItemHandlerSimple(2);
        require(helper, transactor.extract(null, 1, 1, false).isEmpty(), "empty inventory returned an item");

        ItemStack apples = new ItemStack(Items.APPLE, 3);
        require(helper, transactor.insert(apples.copy(), false, true).isEmpty(), "simulated insert returned overflow");
        require(helper, transactor.extract(null, 1, 3, true).isEmpty(), "simulated insert mutated inventory");

        require(helper, transactor.insert(apples.copy(), false, false).isEmpty(), "executed insert returned overflow");
        ItemStack simulated = transactor.extract(null, 2, 2, true);
        require(helper, ItemStack.isSameItemSameComponents(apples, simulated), "simulated extraction returned wrong item");
        require(helper, simulated.getCount() == 2, "simulated extraction returned wrong count");

        ItemStack extracted = transactor.extract(null, 1, 3, false);
        require(helper, ItemStack.isSameItemSameComponents(apples, extracted), "executed extraction returned wrong item");
        require(helper, extracted.getCount() == 3, "executed extraction returned wrong count");
        require(helper, transactor.extract(null, 1, 1, false).isEmpty(), "inventory was not empty after extraction");
        helper.succeed();
    }

    @GameTest(templateNamespace = BCLib.MODID, template = EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void itemTransactorLimitedInventoryReturnsOverflow(GameTestHelper helper) {
        IItemTransactor limited = new ItemHandlerSimple(
            2,
            (slot, stack) -> true,
            StackInsertionFunction.getInsertionFunction(4),
            null
        );

        ItemStack input = new ItemStack(Items.APPLE, 9);
        ItemStack before = input.copy();
        ItemStack leftover = limited.insert(input, false, false);

        require(helper, ItemStack.matches(before, input), "insert changed its input stack");
        require(helper, leftover.getCount() == 1, "limited inventory returned wrong overflow count");
        require(helper, ItemStack.isSameItemSameComponents(input, leftover), "overflow item differs from input");
        require(helper, limited.extract(null, 8, 8, true).getCount() == 8, "limited inventory stored wrong count");
        helper.succeed();
    }

    @GameTest(templateNamespace = BCLib.MODID, template = EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void itemTransactorNbtRoundTrip(GameTestHelper helper) {
        ItemHandlerSimple original = new ItemHandlerSimple(3);
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        pickaxe.setDamageValue(17);
        pickaxe.set(DataComponents.CUSTOM_NAME, Component.literal("test pickaxe"));
        original.setStackInSlot(1, pickaxe);
        original.setStackInSlot(2, new ItemStack(Items.APPLE, 12));

        CompoundTag nbt = original.serializeNBT(helper.getLevel().registryAccess());
        ItemHandlerSimple restored = new ItemHandlerSimple(3);
        restored.deserializeNBT(helper.getLevel().registryAccess(), nbt);

        require(helper, restored.getStackInSlot(0).isEmpty(), "empty slot was not preserved");
        require(helper, ItemStack.matches(pickaxe, restored.getStackInSlot(1)), "tagged pickaxe was not preserved");
        require(helper, restored.getStackInSlot(2).getCount() == 12, "apple count was not preserved");
        require(helper, restored.getStackInSlot(2).getItem() == Items.APPLE, "apple item was not preserved");
        helper.succeed();
    }

    @GameTest(templateNamespace = BCLib.MODID, template = EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void toolMatcherGroupsToolsByAction(GameTestHelper helper) {
        ListMatchHandlerTools matcher = new ListMatchHandlerTools();
        ItemStack woodenAxe = new ItemStack(Items.WOODEN_AXE);
        ItemStack ironAxe = new ItemStack(Items.IRON_AXE);
        ItemStack damagedWoodenAxe = new ItemStack(Items.WOODEN_AXE);
        damagedWoodenAxe.setDamageValue(26);
        ItemStack woodenShovel = new ItemStack(Items.WOODEN_SHOVEL);
        ItemStack apple = new ItemStack(Items.APPLE);

        require(helper, matcher.isValidSource(ListMatchType.TYPE, woodenAxe), "wooden axe is not a valid TYPE source");
        require(helper, matcher.isValidSource(ListMatchType.TYPE, damagedWoodenAxe), "damaged axe is not a valid TYPE source");
        require(helper, !matcher.isValidSource(ListMatchType.TYPE, apple), "apple is a valid tool TYPE source");
        require(helper, matcher.matches(ListMatchType.TYPE, woodenAxe, ironAxe, false), "axe materials did not match by TYPE");
        require(helper, matcher.matches(ListMatchType.TYPE, woodenAxe, damagedWoodenAxe, false), "axe damage changed TYPE match");
        require(helper, !matcher.matches(ListMatchType.TYPE, woodenAxe, woodenShovel, false), "axe matched shovel by TYPE");
        require(helper, !matcher.matches(ListMatchType.TYPE, woodenAxe, apple, false), "axe matched apple by TYPE");
        require(helper, !matcher.matches(ListMatchType.MATERIAL, woodenAxe, ironAxe, false), "different materials matched");
        helper.succeed();
    }

    @GameTest(templateNamespace = BCLib.MODID, template = EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void packetBooleansPackAcrossPrimitiveWrites(GameTestHelper helper) {
        PacketBufferBC buffer = new PacketBufferBC(Unpooled.buffer());
        try {
            buffer.writeInt(49);
            buffer.writeBoolean(true);
            buffer.writeShort(95);
            buffer.writeBoolean(false);
            buffer.writeByte(11);
            buffer.writeBoolean(true);

            byte[] expected = { 0, 0, 0, 49, 5, 0, 95, 11 };
            byte[] actual = new byte[expected.length];
            buffer.getBytes(0, actual);
            require(helper, Arrays.equals(expected, actual), "packed packet bytes differ");

            require(helper, buffer.readInt() == 49, "int round-trip failed");
            require(helper, buffer.readBoolean(), "first boolean round-trip failed");
            require(helper, buffer.readShort() == 95, "short round-trip failed");
            require(helper, !buffer.readBoolean(), "second boolean round-trip failed");
            require(helper, buffer.readByte() == 11, "byte round-trip failed");
            require(helper, buffer.readBoolean(), "third boolean round-trip failed");
            helper.succeed();
        } finally {
            buffer.release();
        }
    }

    @GameTest(templateNamespace = BCLib.MODID, template = EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void packetFixedBitsRoundTrip(GameTestHelper helper) {
        PacketBufferBC buffer = new PacketBufferBC(Unpooled.buffer());
        try {
            buffer.writeFixedBits(0xA4, 10);
            buffer.writeFixedBits(1, 2);
            buffer.writeBoolean(true);
            buffer.writeFixedBits(0xF_81_67, 20);
            buffer.writeFixedBits(0x7E_DC_A9_87, 31);

            require(helper, buffer.readFixedBits(10) == 0xA4, "10-bit value round-trip failed");
            require(helper, buffer.readFixedBits(2) == 1, "2-bit value round-trip failed");
            require(helper, buffer.readBoolean(), "interleaved boolean round-trip failed");
            require(helper, buffer.readFixedBits(20) == 0xF_81_67, "20-bit value round-trip failed");
            require(helper, buffer.readFixedBits(31) == 0x7E_DC_A9_87, "31-bit value round-trip failed");
            require(helper, buffer.readerIndex() == 8 && buffer.writerIndex() == 8, "packet indexes differ after round-trip");
            helper.succeed();
        } finally {
            buffer.release();
        }
    }

    @GameTest(templateNamespace = BCLib.MODID, template = EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void packetEnumsUseCompactBitEncoding(GameTestHelper helper) {
        PacketBufferBC buffer = new PacketBufferBC(Unpooled.buffer());
        try {
            buffer.writeBoolean(true);
            buffer.writeEnum(Direction.DOWN);
            buffer.writeEnum(Direction.SOUTH);
            buffer.writeEnum(DyeColor.BROWN);
            buffer.writeEnum(DyeColor.CYAN);

            require(helper, buffer.readBoolean(), "leading boolean round-trip failed");
            require(helper, buffer.readEnum(Direction.class) == Direction.DOWN, "first direction round-trip failed");
            require(helper, buffer.readEnum(Direction.class) == Direction.SOUTH, "second direction round-trip failed");
            require(helper, buffer.readEnum(DyeColor.class) == DyeColor.BROWN, "first dye round-trip failed");
            require(helper, buffer.readEnum(DyeColor.class) == DyeColor.CYAN, "second dye round-trip failed");
            require(helper, buffer.readerIndex() == 2 && buffer.writerIndex() == 2, "enum encoding is not compact");
            helper.succeed();
        } finally {
            buffer.release();
        }
    }

    @GameTest(templateNamespace = BCLib.MODID, template = EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void packetInvalidBitLengthsAreRejected(GameTestHelper helper) {
        PacketBufferBC buffer = new PacketBufferBC(Unpooled.buffer());
        try {
            requireThrows(helper, () -> buffer.writeFixedBits(0, 0), "write accepted zero bits");
            requireThrows(helper, () -> buffer.writeFixedBits(0, 33), "write accepted more than 32 bits");
            requireThrows(helper, () -> buffer.readFixedBits(0), "read accepted zero bits");
            requireThrows(helper, () -> buffer.readFixedBits(33), "read accepted more than 32 bits");
            helper.succeed();
        } finally {
            buffer.release();
        }
    }


    @GameTest(templateNamespace = BCLib.MODID, template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void apiV2ItemTransferResultUsesDefensiveCopies(GameTestHelper helper) {
        ItemStack offered = new ItemStack(Items.STICK, 3);
        ItemTransferResult result = ItemTransferResult.ofInsertion(offered, 2);
        offered.setCount(1);

        require(helper, result.transferredCount() == 2, "mutating offered stack changed stored transfer result");
        require(helper, result.remainderCount() == 1, "unexpected insertion remainder");

        ItemStack returned = result.transferred();
        returned.setCount(99);
        require(helper, result.transferredCount() == 2, "mutating returned stack changed stored transfer result");
        helper.succeed();
    }

    @GameTest(templateNamespace = BCLib.MODID, template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void apiV2FacadeRulesUsePriorityAndDefensiveCopies(GameTestHelper helper) {
        FacadeRuleRegistryImpl rules = new FacadeRuleRegistryImpl();
        Block block = Blocks.STONE;
        BlockState state = block.defaultBlockState();

        rules.disable(id("disable"), block, new DefinitionProvenance("addon", "code", 0));
        require(helper, rules.disabledBy(block).isPresent(), "disabled facade rule was not retained");
        require(helper, "addon".equals(rules.disabledBy(block).orElseThrow().owner()), "wrong facade rule provenance");

        ItemStack stack = new ItemStack(Items.STICK, 1);
        rules.mapState(id("map_low"), state, stack, new DefinitionProvenance("low", "code", 0));
        rules.mapState(id("map_high"), state, new ItemStack(Items.STICK, 2), new DefinitionProvenance("high", "code", 10));
        ItemStack first = rules.mappedStack(state).orElseThrow();
        require(helper, first.getCount() == 2, "higher-priority facade mapping did not win");
        first.setCount(99);
        require(helper, rules.mappedStack(state).orElseThrow().getCount() == 2, "facade mapped stack was not defensively copied");
        helper.succeed();
    }

    private static ResourceLocation id(String path) {
        return Objects.requireNonNull(ResourceLocation.tryParse("test:" + path));
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }

    private static void requireThrows(GameTestHelper helper, Runnable action, String message) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        helper.fail(message);
    }

    private static IStatementParameter[] defaultParameters(IFillerPatternShape pattern) {
        IStatementParameter[] parameters = new IStatementParameter[pattern.maxParameters()];
        for (int index = 0; index < parameters.length; index++) {
            parameters[index] = pattern.createParameter(index);
        }
        return parameters;
    }

    private static IFilledTemplate createFilledTemplate(BlockPos size) {
        Template template = new Template();
        template.size = size;
        template.offset = BlockPos.ZERO;
        template.data = new BitSet(Snapshot.getDataSize(size));
        return template.getFilledTemplate();
    }
}
