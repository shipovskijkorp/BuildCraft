package buildcraft.lib.internal.api.v2;

import buildcraft.api.v2.BuildCraftApi;
import buildcraft.api.v2.BuildCraftRegistries;
import buildcraft.api.v2.BuildCraftServices;
import buildcraft.api.v2.content.BuildCraftContentIds;
import buildcraft.api.v2.energy.MjAmount;
import buildcraft.api.v2.machine.BuiltInMachineProperties;
import buildcraft.api.v2.machine.MachineComponent;
import buildcraft.api.v2.machine.MachineComponentType;
import buildcraft.api.v2.machine.MachineType;
import buildcraft.api.v2.registry.ApiRegistry;
import buildcraft.api.v2.registry.RegistrationContext;
import buildcraft.api.v2.worldgen.ResourceDepositRule;
import buildcraft.api.v2.worldgen.WorldTargetSelector;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** Registers BCCE's own content through the same API 2 contracts exposed to addons. */
public final class BuiltInApi2Content {
    private static final RegistrationContext BUILTIN = () -> "buildcraft";
    private static boolean registered;

    private BuiltInApi2Content() {}

    public static synchronized void register() {
        if (registered) return;
        registerMachineComponents();
        registerMachines();
        registerWorldgen();
        registered = true;
    }

    private static void registerMachineComponents() {
        ApiRegistry<MachineComponentType<?>> registry = BuildCraftApi.registry(BuildCraftRegistries.MACHINE_COMPONENT_TYPES);
        for (ResourceLocation id : List.of(
            BuildCraftContentIds.MachineComponents.ENERGY,
            BuildCraftContentIds.MachineComponents.AREA,
            BuildCraftContentIds.MachineComponents.MINING,
            BuildCraftContentIds.MachineComponents.PUMPING,
            BuildCraftContentIds.MachineComponents.DISTILLATION,
            BuildCraftContentIds.MachineComponents.INVENTORY_OUTPUT,
            BuildCraftContentIds.MachineComponents.FLUID_INPUT,
            BuildCraftContentIds.MachineComponents.FLUID_OUTPUT,
            BuildCraftContentIds.MachineComponents.CHUNK_LOADING
        )) {
            registry.register(id, new MachineComponentType<MachineComponent>(id, null), BUILTIN);
        }
    }

    private static void registerMachines() {
        ApiRegistry<MachineType> registry = BuildCraftApi.registry(BuildCraftRegistries.MACHINE_TYPES);
        registry.register(BuildCraftContentIds.Machines.QUARRY, MachineType.builder(BuildCraftContentIds.Machines.QUARRY)
            .component(BuildCraftContentIds.MachineComponents.ENERGY)
            .component(BuildCraftContentIds.MachineComponents.AREA)
            .component(BuildCraftContentIds.MachineComponents.MINING)
            .component(BuildCraftContentIds.MachineComponents.INVENTORY_OUTPUT)
            .component(BuildCraftContentIds.MachineComponents.CHUNK_LOADING)
            .property(BuiltInMachineProperties.WORK_SPEED_MULTIPLIER, 1.0)
            .property(BuiltInMachineProperties.ENERGY_COST_MULTIPLIER, 1.0)
            .property(BuiltInMachineProperties.MAX_MJ_INPUT_PER_TICK, MjAmount.ofMj(512))
            .property(BuiltInMachineProperties.MJ_CAPACITY, MjAmount.ofMj(24_000))
            .property(BuiltInMachineProperties.CHUNK_LOADING, true)
            .build(), BUILTIN);

        registry.register(BuildCraftContentIds.Machines.DISTILLER, MachineType.builder(BuildCraftContentIds.Machines.DISTILLER)
            .component(BuildCraftContentIds.MachineComponents.ENERGY)
            .component(BuildCraftContentIds.MachineComponents.FLUID_INPUT)
            .component(BuildCraftContentIds.MachineComponents.FLUID_OUTPUT)
            .component(BuildCraftContentIds.MachineComponents.DISTILLATION)
            .property(BuiltInMachineProperties.WORK_SPEED_MULTIPLIER, 1.0)
            .property(BuiltInMachineProperties.ENERGY_COST_MULTIPLIER, 1.0)
            .property(BuiltInMachineProperties.MAX_MJ_INPUT_PER_TICK, MjAmount.ofMj(6))
            .property(BuiltInMachineProperties.MJ_CAPACITY, MjAmount.ofMj(1_024))
            .build(), BUILTIN);

        registry.register(BuildCraftContentIds.Machines.MINING_WELL, MachineType.builder(BuildCraftContentIds.Machines.MINING_WELL)
            .component(BuildCraftContentIds.MachineComponents.ENERGY)
            .component(BuildCraftContentIds.MachineComponents.MINING)
            .component(BuildCraftContentIds.MachineComponents.INVENTORY_OUTPUT)
            .property(BuiltInMachineProperties.WORK_SPEED_MULTIPLIER, 1.0)
            .property(BuiltInMachineProperties.ENERGY_COST_MULTIPLIER, 1.0)
            .property(BuiltInMachineProperties.MJ_CAPACITY, MjAmount.ofMj(500))
            .build(), BUILTIN);

        registry.register(BuildCraftContentIds.Machines.PUMP, MachineType.builder(BuildCraftContentIds.Machines.PUMP)
            .component(BuildCraftContentIds.MachineComponents.ENERGY)
            .component(BuildCraftContentIds.MachineComponents.PUMPING)
            .component(BuildCraftContentIds.MachineComponents.FLUID_OUTPUT)
            .property(BuiltInMachineProperties.WORK_SPEED_MULTIPLIER, 1.0)
            .property(BuiltInMachineProperties.ENERGY_COST_MULTIPLIER, 1.0)
            .property(BuiltInMachineProperties.MJ_CAPACITY, MjAmount.ofMj(50))
            .build(), BUILTIN);
    }

    private static void registerWorldgen() {
        ResourceLocation overworld = new ResourceLocation("minecraft", "overworld");
        ResourceDepositRule rule = ResourceDepositRule.builder(
                new ResourceLocation("buildcraftenergy", "standard_oil_overworld"),
                BuildCraftContentIds.Worldgen.STANDARD_OIL
            )
            .target(WorldTargetSelector.builder().dimension(overworld).build())
            .frequencyMultiplier(1.0)
            .priority(0)
            .build();
        BuildCraftApi.service(BuildCraftServices.WORLDGEN).register(rule, BUILTIN);
    }
}
