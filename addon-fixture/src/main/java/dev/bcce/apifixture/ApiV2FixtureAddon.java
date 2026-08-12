package dev.bcce.apifixture;

import buildcraft.api.v2.BuildCraftApi;
import buildcraft.api.v2.BuildCraftRegistries;
import buildcraft.api.v2.BuildCraftServices;
import buildcraft.api.v2.robot.RobotEventListener;
import buildcraft.api.v2.robot.RobotEventDecision;
import buildcraft.api.v2.map.MapLocationView;
import buildcraft.api.v2.map.MapLocationAdapter;
import buildcraft.api.v2.item.ItemLabelAdapter;
import buildcraft.api.v2.energy.MjConnectionRule;
import buildcraft.api.v2.drop.FluidDropProvider;
import buildcraft.api.v2.debug.DebugContributor;
import buildcraft.api.v2.client.ParameterPresentation;
import buildcraft.api.v2.OperationMode;
import buildcraft.api.v2.automation.AutomationActionType;
import buildcraft.api.v2.automation.AutomationRequest;
import buildcraft.api.v2.automation.AutomationResult;
import buildcraft.api.v2.automation.StripesHandler;
import buildcraft.api.v2.client.ContentPresentation;
import buildcraft.api.v2.energy.MjAmount;
import buildcraft.api.v2.fuels.CoolantProfile;
import buildcraft.api.v2.fuels.EnergyFluidService;
import buildcraft.api.v2.fuels.FluidSelector;
import buildcraft.api.v2.fuels.FuelProfile;
import buildcraft.api.v2.fluid.FluidVariant;
import buildcraft.api.v2.fluid.FluidVolume;
import buildcraft.api.v2.machine.EngineProfile;
import buildcraft.api.v2.machine.EngineType;
import buildcraft.api.v2.machine.MachineType;
import buildcraft.api.v2.persistence.ApiCodec;
import buildcraft.api.v2.persistence.CodecResult;
import buildcraft.api.v2.persistence.OpaqueData;
import buildcraft.api.v2.permission.AutomationActor;
import buildcraft.api.v2.permission.PermissionDecision;
import buildcraft.api.v2.permission.PermissionServiceRegistry;
import buildcraft.api.v2.pipe.ItemTransportProfile;
import buildcraft.api.v2.pipe.PipeType;
import buildcraft.api.v2.recipe.DistillationRecipeDefinition;
import buildcraft.api.v2.recipe.FluidIngredient;
import buildcraft.api.v2.recipe.MachineRecipeService;
import buildcraft.api.v2.registry.ApiRegistry;
import buildcraft.api.v2.reload.DefinitionProvenance;
import buildcraft.api.v2.robot.BuildCraftRobotBoards;
import buildcraft.api.v2.robot.RobotBoardType;
import buildcraft.api.v2.robot.RobotTask;
import buildcraft.api.v2.robot.RobotTaskContext;
import buildcraft.api.v2.robot.RobotTaskResult;
import buildcraft.api.v2.signal.BuildCraftSignalChannels;
import buildcraft.api.v2.signal.SignalChannelType;
import buildcraft.api.v2.statement.ActionType;
import buildcraft.api.v2.statement.ParameterSchema;
import buildcraft.api.v2.statement.ParameterSpec;
import buildcraft.api.v2.statement.ParameterType;
import buildcraft.api.v2.statement.StatementResult;
import buildcraft.api.v2.statement.StatementContributor;
import buildcraft.api.v2.statement.TriggerType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;

/**
 * Compile-only consumer proving that third-party common code can use the whole
 * API 2 migration surface without BuildCraft implementation or loader imports.
 */
public final class ApiV2FixtureAddon {
    private static final ResourceLocation BOOL_FORMAT = id("bool_payload");
    private static final ResourceLocation BOOL_PARAMETER_ID = id("boolean_parameter");
    private static final ResourceLocation BOOL_PARAMETER_SLOT = id("enabled");
    private static final ApiCodec<Boolean, OpaqueData> BOOL_CODEC = new ApiCodec<>() {
        @Override
        public CodecResult<Boolean> decode(OpaqueData payload) {
            byte[] bytes = payload.bytes();
            return bytes.length == 1
                ? CodecResult.success(bytes[0] != 0)
                : CodecResult.failure("Expected one byte");
        }

        @Override
        public CodecResult<OpaqueData> encode(Boolean value) {
            return CodecResult.success(new OpaqueData(BOOL_FORMAT, new byte[] { (byte) (value ? 1 : 0) }));
        }
    };
    private static final ParameterType<Boolean> BOOL_PARAMETER = new ParameterType<>(
        BOOL_PARAMETER_ID, BOOL_CODEC, context -> List.of(Boolean.TRUE, Boolean.FALSE)
    );

    private ApiV2FixtureAddon() {}

    public static void registerExamples() {
        registerFoundationExamples();
        registerTransportAndContentExamples();
        registerMigrationEdgeExamples();
    }

    private static void registerFoundationExamples() {
        EnergyFluidService energyFluids = BuildCraftApi.runtime().requireService(BuildCraftServices.ENERGY_FLUIDS);
        PermissionServiceRegistry permissions = BuildCraftApi.runtime().requireService(BuildCraftServices.PERMISSIONS);
        MachineRecipeService recipes = BuildCraftApi.runtime().requireService(BuildCraftServices.MACHINE_RECIPES);
        DefinitionProvenance provenance = new DefinitionProvenance("api-v2-fixture", "fixture-code", 0);

        permissions.register(id("permission_provider"), 0, context -> PermissionDecision.pass());
        energyFluids.register(id("fixture_fuel"), FuelProfile.clean(FluidSelector.fluid(id("fixture_oil")), 2_000_000L, 12_000), provenance);
        energyFluids.register(id("fixture_coolant"), CoolantProfile.constant(FluidSelector.tag(id("fixture_coolants")), 0.003), provenance);

        FluidVariant crude = FluidVariant.of(id("fixture_crude"));
        FluidVariant light = FluidVariant.of(id("fixture_light_fraction"));
        recipes.register(
            id("fixture_distillation"),
            new DistillationRecipeDefinition(
                FluidIngredient.exact(crude, 100),
                FluidVolume.of(light, 40),
                FluidVolume.empty(),
                500_000L
            ),
            provenance
        );
    }

    private static void registerTransportAndContentExamples() {
        ApiRegistry<PipeType> pipes = BuildCraftApi.runtime().requireRegistry(BuildCraftRegistries.PIPE_TYPES);
        pipes.register(
            id("brass_item_pipe"),
            PipeType.builder(id("brass_item_pipe"))
                .itemProfile(new ItemTransportProfile(16, 10))
                .build(),
            () -> "api-v2-fixture"
        );

        ApiRegistry<SignalChannelType<?>> signals = BuildCraftApi.runtime().requireRegistry(BuildCraftRegistries.SIGNAL_CHANNEL_TYPES);
        signals.register(
            id("logic_signal"),
            new SignalChannelType<>(id("logic_signal"), false, BOOL_CODEC, (current, incoming) -> current || incoming),
            () -> "api-v2-fixture"
        );

        BuildCraftApi.runtime().requireRegistry(BuildCraftRegistries.AUTOMATION_ACTION_TYPES).register(
            FixtureAutomationRequest.KIND,
            new AutomationActionType<>(
                FixtureAutomationRequest.KIND,
                FixtureAutomationRequest.class,
                request -> AutomationResult.success(1)
            ),
            () -> "api-v2-fixture"
        );
        BuildCraftApi.runtime().requireRegistry(BuildCraftRegistries.STRIPES_HANDLERS).register(
            id("fixture_stripes"),
            (StripesHandler) context -> {
                int consumed = context.hasItem() ? context.consume(1) : 0;
                return consumed > 0 ? AutomationResult.success(consumed) : AutomationResult.pass();
            },
            () -> "api-v2-fixture"
        );

        BuildCraftApi.runtime().requireRegistry(BuildCraftRegistries.PARAMETER_TYPES).register(
            BOOL_PARAMETER_ID,
            BOOL_PARAMETER,
            () -> "api-v2-fixture"
        );
        BuildCraftApi.runtime().requireRegistry(BuildCraftRegistries.TRIGGER_TYPES).register(
            id("always"),
            new TriggerType(
                id("always"),
                new ParameterSchema(List.of(new ParameterSpec(BOOL_PARAMETER_SLOT, BOOL_PARAMETER_ID, false))),
                (context, parameters) -> parameters.get(BOOL_PARAMETER_SLOT, BOOL_PARAMETER).orElse(Boolean.TRUE)
            ),
            () -> "api-v2-fixture"
        );
        BuildCraftApi.runtime().requireRegistry(BuildCraftRegistries.ACTION_TYPES).register(
            id("noop"),
            new ActionType(id("noop"), ParameterSchema.EMPTY, (context, parameters) -> StatementResult.success()),
            () -> "api-v2-fixture"
        );

        BuildCraftApi.runtime().requireRegistry(BuildCraftRegistries.STATEMENT_CONTRIBUTORS).register(
            id("gate_examples"),
            (StatementContributor) (context, collector) -> {
                collector.addTrigger(id("always"));
                collector.addAction(id("noop"));
            },
            () -> "api-v2-fixture"
        );

        BuildCraftApi.runtime().requireRegistry(BuildCraftRegistries.MACHINE_TYPES).register(
            id("fixture_machine"),
            new MachineType(id("fixture_machine"), Set.of()),
            () -> "api-v2-fixture"
        );
        BuildCraftApi.runtime().requireRegistry(BuildCraftRegistries.ENGINE_TYPES).register(
            id("fixture_engine"),
            new EngineType(id("fixture_engine"), new EngineProfile(MjAmount.ofMj(4), MjAmount.ofMj(100), false)),
            () -> "api-v2-fixture"
        );
        BuildCraftApi.runtime().requireRegistry(BuildCraftRegistries.ROBOT_BOARD_TYPES).register(
            id("fixture_board"),
            new RobotBoardType(id("fixture_board"), 1, Set.of(id("fixture_task"))),
            () -> "api-v2-fixture"
        );
        BuildCraftApi.runtime().requireRegistry(BuildCraftRegistries.CLIENT_PRESENTATIONS).register(
            id("fixture_machine"),
            new ContentPresentation(id("fixture_machine"), "fixture.machine", "fixture.machine.desc", null, id("fixture_machine_model")),
            () -> "api-v2-fixture"
        );
    }

    private static void registerMigrationEdgeExamples() {
        BuildCraftApi.registry(BuildCraftRegistries.MJ_CONNECTION_RULES).register(
            id("same_network"),
            context -> context.local().networkId().equals(context.remote().networkId()),
            () -> "api-v2-fixture"
        );
        BuildCraftApi.registry(BuildCraftRegistries.ITEM_LABEL_ADAPTERS).register(
            id("labels"),
            new ItemLabelAdapter() {
                @Override public boolean supports(ItemStack stack) { return false; }
                @Override public String label(ItemStack stack) { return ""; }
                @Override public boolean setLabel(ItemStack stack, String label, OperationMode mode) { return false; }
            },
            () -> "api-v2-fixture"
        );
        BuildCraftApi.registry(BuildCraftRegistries.MAP_LOCATION_ADAPTERS).register(
            id("map_locations"),
            new MapLocationAdapter() {
                @Override public boolean supports(ItemStack stack) { return false; }
                @Override public Optional<MapLocationView> read(ItemStack stack) { return Optional.empty(); }
                @Override public boolean write(ItemStack stack, MapLocationView location, OperationMode mode) { return false; }
                @Override public boolean clear(ItemStack stack, OperationMode mode) { return false; }
            },
            () -> "api-v2-fixture"
        );
        BuildCraftApi.registry(BuildCraftRegistries.FLUID_DROP_PROVIDERS).register(
            id("fluid_drops"),
            context -> List.of(),
            () -> "api-v2-fixture"
        );
        BuildCraftApi.registry(BuildCraftRegistries.DEBUG_CONTRIBUTORS).register(
            id("debug"),
            context -> List.of(),
            () -> "api-v2-fixture"
        );
        BuildCraftApi.registry(BuildCraftRegistries.ROBOT_EVENT_LISTENERS).register(
            id("robot_events"),
            context -> RobotEventDecision.PASS,
            () -> "api-v2-fixture"
        );
        BuildCraftApi.registry(BuildCraftRegistries.PARAMETER_PRESENTATIONS).register(
            id("boolean_parameter"),
            new ParameterPresentation(id("boolean_parameter"), "fixture.boolean", "", null, id("toggle_editor")),
            () -> "api-v2-fixture"
        );
    }

    /**
     * Compile-only proof that addon common code can discover live BuildCraft MJ ports,
     * machines and laser targets without importing engine/tile/capability implementation classes.
     */
    public static void probeRuntime(Level level, BlockPos pos, Direction side) {
        BuildCraftApi.service(BuildCraftServices.ENERGY).port(level, pos, side);
        BuildCraftApi.service(BuildCraftServices.ENERGY).descriptor(level, pos, side);
        BuildCraftApi.service(BuildCraftServices.MACHINES).machine(level, pos);
        BuildCraftApi.service(BuildCraftServices.LASER_TARGETS).target(level, pos, side);
        BuildCraftApi.service(BuildCraftServices.SIGNALS)
            .port(level, pos, side, BuildCraftSignalChannels.RED)
            .ifPresent(port -> port.connected());
        BuildCraftApi.service(BuildCraftServices.AUTOMATION).execute(
            new FixtureAutomationRequest(pos, AutomationActor.unknown(), OperationMode.SIMULATE)
        );
        // Robotics/request runtime is discoverable without importing EntityRobotBase, AIRobot or DockingStation.
        BuildCraftApi.service(BuildCraftServices.ROBOTS).robots(level).stream().findFirst().ifPresent(robot ->
            robot.control().ifPresent(control -> control.assign(new FixtureRobotTask(), OperationMode.SIMULATE))
        );
        BuildCraftApi.service(BuildCraftServices.ROBOTS).dock(level, pos, side);
        BuildCraftApi.service(BuildCraftServices.REQUESTS).provider(level, pos, side)
            .ifPresent(provider -> provider.requests().size());
        BuildCraftApi.registry(BuildCraftRegistries.ROBOT_BOARD_TYPES).get(BuildCraftRobotBoards.PICKER);
    }

    private static final class FixtureRobotTask implements RobotTask {
        private static final ResourceLocation TYPE = id("fixture_robot_task");
        @Override public ResourceLocation typeId() { return TYPE; }
        @Override public RobotTaskResult tick(RobotTaskContext context) { return RobotTaskResult.complete(); }
    }

    private record FixtureAutomationRequest(
        BlockPos origin, AutomationActor actor, OperationMode mode
    ) implements AutomationRequest {
        private static final ResourceLocation KIND = id("fixture_automation");
        @Override public ResourceLocation kind() { return KIND; }
    }

    private static ResourceLocation id(String path) {
        return Objects.requireNonNull(ResourceLocation.tryParse("api_v2_fixture:" + path));
    }
}
