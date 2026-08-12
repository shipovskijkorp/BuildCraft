package buildcraft.transport.api2;

import buildcraft.api.v2.BuildCraftApi;
import buildcraft.api.v2.BuildCraftRegistries;
import buildcraft.api.v2.energy.MjAmount;
import buildcraft.api.v2.fluid.FluidAmount;
import buildcraft.api.v2.pipe.ExternalEnergyTransportProfile;
import buildcraft.api.v2.pipe.FluidTransportProfile;
import buildcraft.api.v2.pipe.ItemTransportProfile;
import buildcraft.api.v2.pipe.PipeComponentType;
import buildcraft.api.v2.pipe.PipeMedium;
import buildcraft.api.v2.pipe.PipeType;
import buildcraft.api.v2.pipe.PowerTransportProfile;
import buildcraft.api.v2.registry.ApiRegistry;
import buildcraft.api.v2.registry.RegistrationContext;
import buildcraft.transport.internal.pipe.PipeApi;
import buildcraft.transport.internal.pipe.PipeDefinition;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Internal bridge making API2 pipe types authoritative for the legacy-compatible runtime implementation. */
public final class PipeTypeBridge {
    private static final String COMPONENT_PREFIX = "runtime_component/";

    private PipeTypeBridge() {}

    public static PipeType ensureRegistered(PipeDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        ApiRegistry<PipeType> registry = BuildCraftApi.registry(BuildCraftRegistries.PIPE_TYPES);
        PipeType existing = registry.get(definition.identifier);
        if (existing != null) {
            definition.setApiType(existing);
            return existing;
        }

        ResourceLocation componentId = runtimeComponentId(definition.identifier);
        ensureRuntimeComponent(componentId, definition.identifier.getNamespace());

        PipeType type = inferType(definition, componentId);
        registry.register(definition.identifier, type, owner(definition.identifier.getNamespace()));
        definition.setApiType(type);
        definition.setRuntimeComponentId(componentId);
        return type;
    }

    public static void registerAlias(ResourceLocation alias, PipeDefinition target) {
        Objects.requireNonNull(alias, "alias");
        Objects.requireNonNull(target, "target");
        ensureRegistered(target);
        BuildCraftApi.registry(BuildCraftRegistries.PIPE_TYPES)
            .registerAlias(alias, target.identifier, owner(target.identifier.getNamespace()));
    }

    public static ResourceLocation runtimeComponentId(ResourceLocation pipeId) {
        return Objects.requireNonNull(ResourceLocation.tryParse(
            pipeId.getNamespace() + ":" + COMPONENT_PREFIX + pipeId.getPath()
        ));
    }

    private static void ensureRuntimeComponent(ResourceLocation id, String owner) {
        ApiRegistry<PipeComponentType<?>> registry = BuildCraftApi.registry(BuildCraftRegistries.PIPE_COMPONENT_TYPES);
        if (registry.get(id) != null) return;
        PipeComponentType<RuntimePipeComponent> type = new PipeComponentType<>(
            id,
            pipe -> new RuntimePipeComponent(id),
            null
        );
        registry.register(id, type, owner(owner));
    }

    private static PipeType inferType(PipeDefinition definition, ResourceLocation runtimeComponent) {
        PipeType.Builder builder = PipeType.builder(definition.identifier).component(runtimeComponent).colorable(definition.canBeColoured);
        if (definition.flowType == PipeApi.flowItems) {
            // Item throughput is governed by the travelling-item implementation. This profile is intentionally
            // conservative metadata rather than a second source of runtime truth.
            return builder.itemProfile(new ItemTransportProfile(64, 1)).build();
        }
        if (definition.flowType == PipeApi.flowFluids) {
            PipeApi.FluidTransferInfo info = PipeApi.getFluidTransferInfo(definition);
            return builder.fluidProfile(new FluidTransportProfile(
                FluidAmount.of(Math.max(0, info.transferPerTick)),
                Math.max(1, (int) Math.ceil(info.transferDelayMultiplier))
            )).build();
        }
        if (definition.flowType == PipeApi.flowPower) {
            PipeApi.PowerTransferInfo info = PipeApi.getPowerTransferInfo(definition);
            return builder.mjProfile(new PowerTransportProfile(
                MjAmount.ofMicro(Math.max(1, info.transferPerTick)),
                MjAmount.ofMicro(Math.max(0, info.lossPerTick)),
                Math.max(0, Math.min(MjAmount.MICRO_MJ_PER_MJ, info.resistancePerTick)),
                info.isReceiver
            )).build();
        }
        if (definition.flowType == PipeApi.flowForgeEnergy) {
            PipeApi.ForgeEnergyTransferInfo info = PipeApi.getForgeEnergyTransferInfo(definition);
            return builder.externalEnergyProfile(new ExternalEnergyTransportProfile(
                Math.max(1, info.transferPerTick),
                info.isReceiver
            )).build();
        }
        return builder.medium(PipeMedium.STRUCTURE).build();
    }

    private static RegistrationContext owner(String namespace) {
        return () -> namespace;
    }
}
