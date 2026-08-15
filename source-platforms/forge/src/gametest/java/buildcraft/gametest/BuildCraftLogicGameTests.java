package buildcraft.gametest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.netty.buffer.Unpooled;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.api.v2.BuildCraftApi;
import buildcraft.api.v2.BuildCraftServices;
import buildcraft.api.v2.OperationMode;
import buildcraft.api.v2.energy.MjAmount;
import buildcraft.builders.BCBuildersBlocks;
import buildcraft.builders.snapshot.BlueprintBuilder;
import buildcraft.builders.snapshot.ITileForBlueprintBuilder;
import buildcraft.builders.snapshot.ITileForTemplateBuilder;
import buildcraft.builders.snapshot.SchematicBlockDefault;
import buildcraft.builders.snapshot.SnapshotBuilder;
import buildcraft.builders.snapshot.TemplateBuilder;
import buildcraft.builders.tile.TileQuarry;
import buildcraft.core.BCCoreBlocks;
import buildcraft.core.BCCoreConfig;
import buildcraft.core.BCCoreItems;
import buildcraft.energy.BCEnergyBlocks;
import buildcraft.energy.tile.TileDynamoMJ;
import buildcraft.energy.tile.TileEngineFE;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.tile.TilePump;
import buildcraft.lib.BCLibConfig;
import buildcraft.lib.internal.core.InvalidInputDataException;
import buildcraft.lib.internal.enums.EnumEngineType;
import buildcraft.lib.internal.mj.MjBattery;
import buildcraft.lib.internal.properties.BuildCraftProperties;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.block.BlockPipeHolder;

import buildcraft.builders.internal.filler.legacy.FillerManager;
import buildcraft.builders.internal.filler.legacy.IFilledTemplate;
import buildcraft.builders.internal.filler.legacy.IFillerPatternShape;
import buildcraft.lib.internal.inventory.IItemTransactor;
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

        CompoundTag nbt = original.serializeNBT();
        TankManager restored = new TankManager(
            new Tank("input", 1_000, null),
            new Tank("output", 2_000, null)
        );
        restored.deserializeNBT(nbt);

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
        require(helper, ItemStack.isSameItemSameTags(apples, simulated), "simulated extraction returned wrong item");
        require(helper, simulated.getCount() == 2, "simulated extraction returned wrong count");

        ItemStack extracted = transactor.extract(null, 1, 3, false);
        require(helper, ItemStack.isSameItemSameTags(apples, extracted), "executed extraction returned wrong item");
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
        require(helper, ItemStack.isSameItemSameTags(input, leftover), "overflow item differs from input");
        require(helper, limited.extract(null, 8, 8, true).getCount() == 8, "limited inventory stored wrong count");
        helper.succeed();
    }

    @GameTest(templateNamespace = BCLib.MODID, template = EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void itemTransactorNbtRoundTrip(GameTestHelper helper) {
        ItemHandlerSimple original = new ItemHandlerSimple(3);
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        pickaxe.setDamageValue(17);
        pickaxe.setHoverName(Component.literal("test pickaxe"));
        original.setStackInSlot(1, pickaxe);
        original.setStackInSlot(2, new ItemStack(Items.APPLE, 12));

        CompoundTag nbt = original.serializeNBT();
        ItemHandlerSimple restored = new ItemHandlerSimple(3);
        restored.deserializeNBT(nbt);

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


    @GameTest(templateNamespace = BCLib.MODID, template = EMPTY_TEMPLATE, timeoutTicks = 40)
    public static void pumpPreservesDetectedInfiniteWaterSource(GameTestHelper helper) {
        boolean previous = BCCoreConfig.pumpsConsumeWater;
        try {
            BCCoreConfig.pumpsConsumeWater = false;
            BlockPos pumpPos = new BlockPos(1, 4, 1);
            BlockPos waterPos = new BlockPos(1, 2, 1);
            BlockPos[] sources = {
                waterPos,
                waterPos.east(),
                waterPos.south(),
                waterPos.east().south()
            };
            for (BlockPos source : sources) {
                helper.setBlock(source.below(), Blocks.STONE.defaultBlockState());
                helper.setBlock(source, Blocks.WATER.defaultBlockState());
            }
            helper.setBlock(pumpPos, BCFactoryBlocks.PUMP_BLOCK.get().defaultBlockState());
            BlockEntity blockEntity = helper.getBlockEntity(pumpPos);
            require(helper, blockEntity instanceof TilePump, "pump block did not create TilePump");
            TilePump pump = (TilePump) blockEntity;

            invoke(pump, "beginQueueBuild");
            for (int i = 0; i < 32 && readBooleanField(pump, "scanInProgress"); i++) {
                invoke(pump, "continueQueueBuild");
            }
            require(helper, !readBooleanField(pump, "scanInProgress"), "pump fluid scan did not finish");
            require(helper, readBooleanField(pump, "isInfiniteWaterSource"), "2x2 vanilla source was not detected as infinite water");

            int sourcesBefore = countWaterSources(helper, sources);
            MjBattery battery = (MjBattery) readField(pump, "battery");
            battery.addPower(20L * MjAmount.MICRO_MJ_PER_MJ, FluidAction.EXECUTE);
            pump.mine();
            Tank tank = (Tank) readField(pump, "tank");

            require(helper, tank.getFluidAmount() == 1_000, "pump did not produce one bucket from the infinite source");
            require(helper, countWaterSources(helper, sources) == sourcesBefore,
                "pump consumed a vanilla infinite-water source while pumpsConsumeWater=false");
        } finally {
            BCCoreConfig.pumpsConsumeWater = previous;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = BCLib.MODID, template = EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void fillerTreatsReplaceableBlocksAsPlacementTargets(GameTestHelper helper) {
        BlockPos target = new BlockPos(1, 1, 1);
        helper.setBlock(target.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(target, Blocks.SNOW.defaultBlockState());
        BlockPos absolute = helper.absolutePos(target);
        ProbeTemplateBuilder builder = new ProbeTemplateBuilder(new ProbeTemplateTile(helper.getLevel(), absolute));

        require(helper, builder.canPlaceAt(absolute), "filler rejected a replaceable snow layer as a placement target");
        helper.setBlock(target, Blocks.STONE.defaultBlockState());
        require(helper, !builder.canPlaceAt(absolute), "filler treated a solid stone block as directly placeable");
        helper.succeed();
    }

    @GameTest(templateNamespace = BCLib.MODID, template = EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void quarryFluidTraversalKeepsWaterPassableButMinesLavaAndWaterloggedBlocks(GameTestHelper helper) {
        TileQuarry quarry = placeQuarry(helper, new BlockPos(1, 1, 1));
        BlockPos water = new BlockPos(3, 1, 1);
        BlockPos lava = new BlockPos(4, 1, 1);
        BlockPos waterlogged = new BlockPos(5, 1, 1);
        helper.setBlock(water, Blocks.WATER.defaultBlockState());
        helper.setBlock(lava, Blocks.LAVA.defaultBlockState());
        helper.setBlock(waterlogged, Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));

        require(helper, invokeBoolean(quarry, "canMoveThrough", helper.absolutePos(water)),
            "quarry drill no longer moves through standalone water");
        require(helper, !invokeBoolean(quarry, "canMoveThrough", helper.absolutePos(lava)),
            "quarry drill incorrectly moves through high-viscosity lava");
        require(helper, invokeBoolean(quarry, "canMine", helper.absolutePos(lava)),
            "quarry silently skips lava instead of handling it as mineable work");
        require(helper, !invokeBoolean(quarry, "canMoveThrough", helper.absolutePos(waterlogged)),
            "quarry incorrectly treats a waterlogged solid block as standalone water");
        require(helper, invokeBoolean(quarry, "canMine", helper.absolutePos(waterlogged)),
            "quarry cannot mine the solid part of a waterlogged block");
        helper.succeed();
    }

    @GameTest(templateNamespace = BCLib.MODID, template = EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void quarryFramePlannerReplacesFluidsAndExcavatesSolidObstacles(GameTestHelper helper) {
        TileQuarry quarry = placeQuarry(helper, new BlockPos(1, 1, 1));
        BlockPos water = new BlockPos(3, 1, 1);
        BlockPos stone = new BlockPos(4, 1, 1);
        BlockPos frame = new BlockPos(5, 1, 1);
        helper.setBlock(water, Blocks.WATER.defaultBlockState());
        helper.setBlock(stone, Blocks.STONE.defaultBlockState());
        helper.setBlock(frame, BCBuildersBlocks.FRAME.get().defaultBlockState());

        require(helper, !invokeBoolean(quarry, "canIgnoreInFrameBox", helper.absolutePos(water)),
            "quarry frame planner classified fluid as a solid obstacle instead of replacing it with frame");
        require(helper, invokeBoolean(quarry, "canIgnoreInFrameBox", helper.absolutePos(stone)),
            "quarry frame planner no longer schedules solid obstacles for excavation");
        require(helper, invokeBoolean(quarry, "canIgnoreInFrameBox", helper.absolutePos(frame)),
            "existing frame unexpectedly stopped being a normal non-fluid frame-box block");
        helper.succeed();
    }

    @GameTest(templateNamespace = BCLib.MODID, template = EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void schematicPlacementHonoursVanillaCanSurvive(GameTestHelper helper) {
        ProbeSchematicBlock schematic = new ProbeSchematicBlock(Blocks.OAK_SAPLING.defaultBlockState());
        BlockPos target = new BlockPos(2, 2, 2);
        BlockPos absolute = helper.absolutePos(target);

        helper.setBlock(target, Blocks.AIR.defaultBlockState());
        helper.setBlock(target.below(), Blocks.STONE.defaultBlockState());
        require(helper, !schematic.canBuild(helper.getLevel(), absolute),
            "schematic allowed a sapling on an invalid support block");
        require(helper, !schematic.build(helper.getLevel(), absolute),
            "schematic built a block whose vanilla state cannot survive");

        helper.setBlock(target.below(), Blocks.DIRT.defaultBlockState());
        require(helper, schematic.canBuild(helper.getLevel(), absolute),
            "schematic rejected a sapling with valid dirt support");
        require(helper, schematic.build(helper.getLevel(), absolute), "schematic failed a valid canSurvive placement");
        require(helper, helper.getLevel().getBlockState(absolute).is(Blocks.OAK_SAPLING),
            "valid schematic placement produced the wrong block");
        helper.succeed();
    }

    @GameTest(templateNamespace = BCLib.MODID, template = EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void schematicLeavesBecomePersistentAfterSerializationRoundTrip(GameTestHelper helper) {
        BlockState capturedLeaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, false);
        ProbeSchematicBlock original = new ProbeSchematicBlock(capturedLeaves);
        ProbeSchematicBlock restored = new ProbeSchematicBlock();
        try {
            restored.deserializeNBT(original.serializeNBT());
        } catch (InvalidInputDataException e) {
            helper.fail("schematic leaves failed NBT round-trip: " + e.getMessage());
            return;
        }

        BlockPos target = new BlockPos(2, 2, 2);
        BlockPos absolute = helper.absolutePos(target);
        helper.setBlock(target, Blocks.AIR.defaultBlockState());
        require(helper, restored.build(helper.getLevel(), absolute), "restored leaf schematic failed to build");
        BlockState placed = helper.getLevel().getBlockState(absolute);
        require(helper, placed.is(Blocks.OAK_LEAVES), "restored leaf schematic placed the wrong block");
        require(helper, placed.getValue(BlockStateProperties.PERSISTENT),
            "blueprint-built leaves were not forced persistent and may decay immediately");
        require(helper, restored.isBuilt(helper.getLevel(), absolute),
            "schematic did not recognise its persistent leaf placement as complete");
        helper.succeed();
    }

    @GameTest(templateNamespace = BCLib.MODID, template = EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void commonWrenchTagToggleControlsExternalTaggedItemsOnly(GameTestHelper helper) {
        boolean previous = BCLibConfig.useWrenchTag;
        try {
            ItemStack externalTaggedWrench = new ItemStack(Items.STICK);
            ItemStack buildCraftWrench = new ItemStack(BCCoreItems.WRENCH.get());
            BCLibConfig.useWrenchTag = true;
            require(helper, BuildCraftApi.service(BuildCraftServices.WRENCHES).isWrench(externalTaggedWrench),
                "GameTest c:tools/wrench item was not accepted while useWrenchTag=true");

            BCLibConfig.useWrenchTag = false;
            require(helper, !BuildCraftApi.service(BuildCraftServices.WRENCHES).isWrench(externalTaggedWrench),
                "external c:tools/wrench item remained accepted while useWrenchTag=false");
            require(helper, BuildCraftApi.service(BuildCraftServices.WRENCHES).isWrench(buildCraftWrench),
                "native BuildCraft wrench was incorrectly disabled together with the common tag");
        } finally {
            BCLibConfig.useWrenchTag = previous;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = BCLib.MODID, template = EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void pipeHolderWaterloggingPreservesTheWaterFluidState(GameTestHelper helper) {
        BlockPos pipePos = new BlockPos(2, 1, 2);
        BlockState waterlogged = BCTransportBlocks.pipeHolder.get().defaultBlockState()
            .setValue(BlockPipeHolder.WATERLOGGED, true);
        helper.setBlock(pipePos, waterlogged);
        BlockPos absolute = helper.absolutePos(pipePos);

        require(helper, helper.getLevel().getBlockState(absolute).getValue(BlockPipeHolder.WATERLOGGED),
            "pipe holder lost WATERLOGGED=true after placement");
        require(helper, helper.getLevel().getFluidState(absolute).isSource()
                && helper.getLevel().getFluidState(absolute).getType() == Fluids.WATER,
            "waterlogged pipe holder does not expose a vanilla water source fluid state");

        helper.setBlock(pipePos.east(), Blocks.STONE.defaultBlockState());
        require(helper, helper.getLevel().getBlockState(absolute).getValue(BlockPipeHolder.WATERLOGGED),
            "pipe holder lost waterlogging after a neighbour update");
        require(helper, helper.getLevel().getFluidState(absolute).getType() == Fluids.WATER,
            "neighbour update removed water from a waterlogged pipe holder");
        helper.succeed();
    }

    @GameTest(templateNamespace = BCLib.MODID, template = EMPTY_TEMPLATE, timeoutTicks = 30)
    public static void feMjConverterRoundTripConservesEnergy(GameTestHelper helper) {
        TileEngineFE engine = placeFeEngine(helper, new BlockPos(1, 1, 1));
        TileDynamoMJ dynamo = placeMjDynamo(helper, new BlockPos(3, 1, 1));
        engine.isRedstonePowered = true;
        dynamo.isRedstonePowered = true;

        long ratio = BuildCraftApi.service(BuildCraftServices.ENERGY).conversion().microMjPerFe();
        require(helper, ratio > 0, "FE/MJ conversion ratio is not positive");
        long inserted = engine.externalEnergyPort(Direction.NORTH).orElseThrow().insert(80, OperationMode.EXECUTE);
        require(helper, inserted == 80, "FE Engine did not accept the test FE input");

        invoke(engine, "burn");
        long generatedMj = engine.getEnergyStored();
        require(helper, generatedMj > 0 && generatedMj % ratio == 0,
            "FE Engine produced an invalid MJ amount for the configured conversion ratio");

        var extracted = engine.mjPort(Direction.UP).orElseThrow()
            .extract(MjAmount.ofMicro(generatedMj), OperationMode.EXECUTE);
        require(helper, extracted.completed(), "FE Engine API2 MJ output did not extract all generated MJ");
        var acceptedMj = dynamo.mjPort(Direction.NORTH).orElseThrow()
            .insert(extracted.transferred(), OperationMode.EXECUTE);
        require(helper, acceptedMj.completed(), "MJ Dynamo API2 input rejected generated MJ");
        invoke(dynamo, "burn");

        require(helper, engine.getEnergyStored() == 0, "FE Engine retained MJ after complete extraction");
        require(helper, dynamo.getMjStored() == 0, "MJ Dynamo retained convertible MJ after burn");
        require(helper, engine.getCurrentFe() + dynamo.getCurrentFe() == 80,
            "FE -> MJ -> FE round-trip created or destroyed integer FE units");
        helper.succeed();
    }

    @GameTest(templateNamespace = BCLib.MODID, template = EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void feMjConverterSimulationDoesNotMutateMachineBuffers(GameTestHelper helper) {
        TileEngineFE engine = placeFeEngine(helper, new BlockPos(1, 1, 1));
        TileDynamoMJ dynamo = placeMjDynamo(helper, new BlockPos(3, 1, 1));
        long offeredMj = 4L * MjAmount.MICRO_MJ_PER_MJ;

        long simulatedFe = engine.externalEnergyPort(Direction.NORTH).orElseThrow().insert(80, OperationMode.SIMULATE);
        require(helper, simulatedFe == 80, "FE Engine simulation reported the wrong accepted amount");
        require(helper, engine.getCurrentFe() == 0, "FE Engine simulation mutated its FE buffer");

        var simulatedMj = dynamo.mjPort(Direction.NORTH).orElseThrow()
            .insert(MjAmount.ofMicro(offeredMj), OperationMode.SIMULATE);
        require(helper, simulatedMj.completed(), "MJ Dynamo simulation reported an unexpected remainder");
        require(helper, dynamo.getMjStored() == 0, "MJ Dynamo simulation mutated its MJ buffer");
        helper.succeed();
    }

    @GameTest(templateNamespace = BCLib.MODID, template = EMPTY_TEMPLATE, timeoutTicks = 30)
    public static void converterMachineStateSurvivesPersistenceRoundTrip(GameTestHelper helper) {
        BlockState engineState = feEngineState();
        BlockState dynamoState = BCEnergyBlocks.DYNAMO_MJ.get().defaultBlockState();
        TileEngineFE engine = placeFeEngine(helper, new BlockPos(1, 1, 1));
        TileDynamoMJ dynamo = placeMjDynamo(helper, new BlockPos(3, 1, 1));
        engine.isRedstonePowered = true;
        dynamo.isRedstonePowered = true;

        engine.externalEnergyPort(Direction.NORTH).orElseThrow().insert(123, OperationMode.EXECUTE);
        invoke(engine, "burn");
        dynamo.mjPort(Direction.NORTH).orElseThrow()
            .insert(MjAmount.ofMicro(6L * MjAmount.MICRO_MJ_PER_MJ), OperationMode.EXECUTE);
        invoke(dynamo, "burn");

        CompoundTag engineTag = saveMachineState(engine, helper);
        CompoundTag dynamoTag = saveMachineState(dynamo, helper);
        TileEngineFE restoredEngine = new TileEngineFE(helper.absolutePos(new BlockPos(5, 1, 1)), engineState);
        TileDynamoMJ restoredDynamo = new TileDynamoMJ(helper.absolutePos(new BlockPos(6, 1, 1)), dynamoState);
        loadMachineState(restoredEngine, engineTag, helper);
        loadMachineState(restoredDynamo, dynamoTag, helper);

        require(helper, restoredEngine.getCurrentFe() == engine.getCurrentFe(), "FE Engine lost FE buffer across persistence round-trip");
        require(helper, restoredEngine.getEnergyStored() == engine.getEnergyStored(), "FE Engine lost MJ buffer across persistence round-trip");
        require(helper, restoredDynamo.getCurrentFe() == dynamo.getCurrentFe(), "MJ Dynamo lost FE buffer across persistence round-trip");
        require(helper, restoredDynamo.getMjStored() == dynamo.getMjStored(), "MJ Dynamo lost MJ buffer across persistence round-trip");
        helper.succeed();
    }


    @GameTest(templateNamespace = BCLib.MODID, template = EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void builderResourceReservationRollsBackAfterMidTransactionFailure(GameTestHelper helper) {
        FailingSecondExtractionTransactor resources = new FailingSecondExtractionTransactor();
        ProbeBlueprintTile tile = new ProbeBlueprintTile(
            helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), resources
        );
        BlueprintBuilder builder = new BlueprintBuilder(tile);

        Object result = invoke(
            builder, "tryExtractRequired",
            List.of(new ItemStack(Items.APPLE), new ItemStack(Items.IRON_INGOT)),
            List.of(),
            false
        );
        require(helper, result instanceof java.util.Optional<?> optional && optional.isEmpty(),
            "builder accepted an incomplete material reservation");
        require(helper, resources.apples == 1,
            "builder did not roll back the first item after a later reservation failed");
        require(helper, resources.iron == 1,
            "failed reservation unexpectedly consumed the second item");
        helper.succeed();
    }

    @GameTest(templateNamespace = BCLib.MODID, template = EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void creativeBuilderCancellationDoesNotMintDisplayRequirements(GameTestHelper helper) {
        FailingSecondExtractionTransactor resources = new FailingSecondExtractionTransactor();
        ProbeBlueprintTile tile = new ProbeBlueprintTile(
            helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), resources, false
        );
        BlueprintBuilder builder = new BlueprintBuilder(tile);
        Object task = builder.new PlaceTask(
            helper.absolutePos(new BlockPos(2, 1, 1)), List.of(new ItemStack(Items.APPLE)), 0
        );

        invoke(builder, "cancelPlaceTask", task);
        require(helper, resources.apples == 1,
            "creative/no-material cancellation refunded a display-only requirement and minted an item");
        helper.succeed();
    }

    @GameTest(templateNamespace = BCLib.MODID, template = EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void quarryCancelledTaskRefundsExactWithdrawnPower(GameTestHelper helper) {
        TileQuarry quarry = placeQuarry(helper, new BlockPos(1, 1, 1));
        MjBattery battery = (MjBattery) readField(quarry, "battery");
        long initial = 5L * MjAmount.MICRO_MJ_PER_MJ;
        battery.addPower(initial, FluidAction.EXECUTE);
        long before = battery.getStored();
        long withdrawn = battery.extractPower(0, MjAmount.MICRO_MJ_PER_MJ);
        require(helper, withdrawn > 0, "quarry test could not withdraw task power");

        Object task = newQuarryTask(quarry, "TaskBreakBlock", helper.absolutePos(new BlockPos(5, 1, 1)));
        boolean cancelled = (Boolean) invoke(task, "addPower", withdrawn, withdrawn);
        require(helper, cancelled, "stale quarry break task did not cancel");
        require(helper, battery.getStored() == before,
            "cancelled quarry task did not refund exactly the battery energy withdrawn for it");
        helper.succeed();
    }


    private static TileQuarry placeQuarry(GameTestHelper helper, BlockPos relativePos) {
        helper.setBlock(relativePos, BCBuildersBlocks.QUARRY.get().defaultBlockState());
        BlockEntity blockEntity = helper.getBlockEntity(relativePos);
        if (!(blockEntity instanceof TileQuarry quarry)) {
            helper.fail("quarry block did not create TileQuarry");
            throw new IllegalStateException("missing TileQuarry");
        }
        return quarry;
    }

    private static TileEngineFE placeFeEngine(GameTestHelper helper, BlockPos relativePos) {
        helper.setBlock(relativePos, feEngineState());
        BlockEntity blockEntity = helper.getBlockEntity(relativePos);
        if (!(blockEntity instanceof TileEngineFE engine)) {
            helper.fail("FE engine block did not create TileEngineFE");
            throw new IllegalStateException("missing TileEngineFE");
        }
        return engine;
    }

    private static TileDynamoMJ placeMjDynamo(GameTestHelper helper, BlockPos relativePos) {
        helper.setBlock(relativePos, BCEnergyBlocks.DYNAMO_MJ.get().defaultBlockState());
        BlockEntity blockEntity = helper.getBlockEntity(relativePos);
        if (!(blockEntity instanceof TileDynamoMJ dynamo)) {
            helper.fail("MJ dynamo block did not create TileDynamoMJ");
            throw new IllegalStateException("missing TileDynamoMJ");
        }
        return dynamo;
    }

    private static BlockState feEngineState() {
        return BCCoreBlocks.ENGINE_BC8.get().defaultBlockState()
            .setValue(BuildCraftProperties.ENGINE_TYPE, EnumEngineType.FE);
    }

    private static int countWaterSources(GameTestHelper helper, BlockPos[] relativePositions) {
        int count = 0;
        for (BlockPos relative : relativePositions) {
            var fluid = helper.getLevel().getFluidState(helper.absolutePos(relative));
            if (fluid.isSource() && fluid.getType() == Fluids.WATER) count++;
        }
        return count;
    }

    private static Object newQuarryTask(TileQuarry quarry, String simpleName, Object argument) {
        for (Class<?> nested : TileQuarry.class.getDeclaredClasses()) {
            if (!nested.getSimpleName().equals(simpleName)) continue;
            for (java.lang.reflect.Constructor<?> constructor : nested.getDeclaredConstructors()) {
                if (constructor.getParameterCount() != 2) continue;
                try {
                    constructor.setAccessible(true);
                    return constructor.newInstance(quarry, argument);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Cannot create quarry task " + simpleName, e);
                }
            }
        }
        throw new IllegalStateException("Missing quarry task " + simpleName);
    }

    private static Object readField(Object target, String name) {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                // Continue through the hierarchy.
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot read " + name + " from " + target.getClass().getName(), e);
            }
        }
        throw new IllegalStateException("Missing field " + name + " on " + target.getClass().getName());
    }

    private static boolean readBooleanField(Object target, String name) {
        return (Boolean) readField(target, name);
    }

    private static Object invoke(Object target, String name, Object... args) {
        Method method = findMethod(target.getClass(), name, args.length);
        try {
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot invoke " + name + " on " + target.getClass().getName(), e);
        }
    }

    private static boolean invokeBoolean(Object target, String name, Object... args) {
        return (Boolean) invoke(target, name, args);
    }

    private static Method findMethod(Class<?> start, String name, int parameterCount) {
        for (Class<?> type = start; type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
                    return method;
                }
            }
        }
        throw new IllegalStateException("Missing method " + name + "/" + parameterCount + " on " + start.getName());
    }

    private static CompoundTag saveMachineState(BlockEntity blockEntity, GameTestHelper helper) {
        CompoundTag tag = new CompoundTag();
        Method oneArg = findMethodOrNull(blockEntity.getClass(), "saveAdditional", 1);
        Method twoArg = findMethodOrNull(blockEntity.getClass(), "saveAdditional", 2);
        try {
            if (oneArg != null) {
                oneArg.setAccessible(true);
                oneArg.invoke(blockEntity, tag);
                return tag;
            }
            if (twoArg != null) {
                twoArg.setAccessible(true);
                twoArg.invoke(blockEntity, tag, helper.getLevel().registryAccess());
                return tag;
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot save machine state for " + blockEntity.getClass().getName(), e);
        }
        throw new IllegalStateException("No saveAdditional method for " + blockEntity.getClass().getName());
    }

    private static void loadMachineState(BlockEntity blockEntity, CompoundTag tag, GameTestHelper helper) {
        Method load = findMethodOrNull(blockEntity.getClass(), "load", 1);
        Method loadAdditional = findMethodOrNull(blockEntity.getClass(), "loadAdditional", 2);
        try {
            if (load != null) {
                load.setAccessible(true);
                load.invoke(blockEntity, tag.copy());
                return;
            }
            if (loadAdditional != null) {
                loadAdditional.setAccessible(true);
                loadAdditional.invoke(blockEntity, tag.copy(), helper.getLevel().registryAccess());
                return;
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot load machine state for " + blockEntity.getClass().getName(), e);
        }
        throw new IllegalStateException("No load method for " + blockEntity.getClass().getName());
    }

    private static Method findMethodOrNull(Class<?> start, String name, int parameterCount) {
        for (Class<?> type = start; type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
                    return method;
                }
            }
        }
        return null;
    }

    private static final class FailingSecondExtractionTransactor implements IItemTransactor {
        private int apples = 1;
        private int iron = 1;
        private int executeExtractions;

        @Override
        public ItemStack insert(ItemStack stack, boolean allOrNone, boolean simulate) {
            if (stack.isEmpty()) return ItemStack.EMPTY;
            if (stack.getItem() == Items.APPLE) {
                if (!simulate) apples += stack.getCount();
                return ItemStack.EMPTY;
            }
            if (stack.getItem() == Items.IRON_INGOT) {
                if (!simulate) iron += stack.getCount();
                return ItemStack.EMPTY;
            }
            return stack;
        }

        @Override
        public ItemStack extract(buildcraft.lib.internal.core.IStackFilter filter, int min, int max, boolean simulate) {
            ItemStack candidate = ItemStack.EMPTY;
            if (apples >= min) {
                ItemStack apple = new ItemStack(Items.APPLE, Math.min(max, apples));
                if (filter == null || filter.matches(apple)) candidate = apple;
            }
            if (candidate.isEmpty() && iron >= min) {
                ItemStack ingot = new ItemStack(Items.IRON_INGOT, Math.min(max, iron));
                if (filter == null || filter.matches(ingot)) candidate = ingot;
            }
            if (candidate.isEmpty()) return ItemStack.EMPTY;
            if (simulate) return candidate;
            executeExtractions++;
            if (executeExtractions == 2) return ItemStack.EMPTY;
            if (candidate.getItem() == Items.APPLE) apples -= candidate.getCount();
            else if (candidate.getItem() == Items.IRON_INGOT) iron -= candidate.getCount();
            return candidate;
        }
    }

    private static final class ProbeBlueprintTile implements ITileForBlueprintBuilder {
        private final Level level;
        private final BlockPos pos;
        private final IItemTransactor resources;
        private final boolean needMaterial;
        private final TankManager tanks = new TankManager(new Tank("transaction", 1_000, null));

        private ProbeBlueprintTile(Level level, BlockPos pos, IItemTransactor resources) {
            this(level, pos, resources, true);
        }

        private ProbeBlueprintTile(Level level, BlockPos pos, IItemTransactor resources, boolean needMaterial) {
            this.level = level;
            this.pos = pos;
            this.resources = resources;
            this.needMaterial = needMaterial;
        }

        @Override public Level getWorldBC() { return level; }
        @Override public MjBattery getBattery() { return null; }
        @Override public BlockPos getBuilderPos() { return pos; }
        @Override public boolean canExcavate() { return true; }
        @Override public SnapshotBuilder<?> getBuilder() { return null; }
        @Override public boolean needMeterial() { return needMaterial; }
        @Override public GameProfile getOwner() { return new GameProfile(new UUID(0L, 2L), "transaction"); }
        @Override public buildcraft.builders.snapshot.Blueprint.BuildingInfo getBlueprintBuildingInfo() { return null; }
        @Override public IItemTransactor getInvResources() { return resources; }
        @Override public TankManager getTankManager() { return tanks; }
    }

    private static final class ProbeTemplateBuilder extends TemplateBuilder {
        private ProbeTemplateBuilder(ITileForTemplateBuilder tile) {
            super(tile);
        }

        private boolean canPlaceAt(BlockPos pos) {
            return canPlace(pos);
        }
    }

    private static final class ProbeTemplateTile implements ITileForTemplateBuilder {
        private final Level level;
        private final BlockPos pos;

        private ProbeTemplateTile(Level level, BlockPos pos) {
            this.level = level;
            this.pos = pos;
        }

        @Override public Level getWorldBC() { return level; }
        @Override public MjBattery getBattery() { return null; }
        @Override public BlockPos getBuilderPos() { return pos; }
        @Override public boolean canExcavate() { return true; }
        @Override public SnapshotBuilder<?> getBuilder() { return null; }
        @Override public boolean needMeterial() { return false; }
        @Override public GameProfile getOwner() { return new GameProfile(new UUID(0L, 1L), "gametest"); }
        @Override public Template.BuildingInfo getTemplateBuildingInfo() { return null; }
        @Override public IItemTransactor getInvResources() { return null; }
    }

    private static final class ProbeSchematicBlock extends SchematicBlockDefault {
        private ProbeSchematicBlock() {
        }

        private ProbeSchematicBlock(BlockState state) {
            this.blockState = state;
            this.placeBlock = state.getBlock();
        }
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
