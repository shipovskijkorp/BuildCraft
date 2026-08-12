package buildcraft.transport.api2;

import buildcraft.api.v2.BuildCraftApi;
import buildcraft.api.v2.BuildCraftRegistries;
import buildcraft.api.v2.OperationMode;
import buildcraft.api.v2.signal.BuildCraftSignalChannels;
import buildcraft.api.v2.signal.SignalChannelType;
import buildcraft.api.v2.signal.SignalEndpoint;
import buildcraft.api.v2.signal.SignalNetworkView;
import buildcraft.api.v2.signal.SignalPort;
import buildcraft.api.v2.signal.SignalService;
import buildcraft.api.v2.signal.SignalUpdateResult;
import buildcraft.transport.tile.TilePipeHolder;
import buildcraft.transport.wire.WireManager;
import buildcraft.transport.wire.WireSystem;
import buildcraft.transport.wire.WorldSavedDataWireSystems;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Runtime bridge from API2 signal ports to the classic BuildCraft pipe-wire graph. */
public final class SignalServiceImpl implements SignalService {
    public static final SignalServiceImpl INSTANCE = new SignalServiceImpl();

    private SignalServiceImpl() {}

    @Override
    public Optional<SignalPort<?>> port(Level level, BlockPos pos, Direction side, ResourceLocation channelId) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(channelId, "channelId");

        DyeColor color = BuildCraftSignalChannels.color(channelId).orElse(null);
        if (color == null) return Optional.empty();
        SignalChannelType<?> rawType = BuildCraftApi.registry(BuildCraftRegistries.SIGNAL_CHANNEL_TYPES).get(channelId);
        if (rawType == null) return Optional.empty();

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof TilePipeHolder holder)) return Optional.empty();
        WireManager manager = holder.getWireManager();

        @SuppressWarnings("unchecked")
        SignalChannelType<Boolean> type = (SignalChannelType<Boolean>) rawType;
        return Optional.of(new ClassicWirePort(manager, side, color, type));
    }

    @Override
    public Collection<? extends SignalNetworkView<?>> networks(Level level) {
        Objects.requireNonNull(level, "level");
        if (level.isClientSide()) return List.of();

        WorldSavedDataWireSystems saved = WorldSavedDataWireSystems.get(level);
        List<SignalNetworkView<?>> result = new ArrayList<>();
        saved.wireSystems.forEach((wireSystem, powered) -> {
            ResourceLocation id = BuildCraftSignalChannels.id(wireSystem.color);
            SignalChannelType<?> rawType = BuildCraftApi.registry(BuildCraftRegistries.SIGNAL_CHANNEL_TYPES).get(id);
            if (rawType == null) return;
            @SuppressWarnings("unchecked")
            SignalChannelType<Boolean> type = (SignalChannelType<Boolean>) rawType;
            LinkedHashSet<SignalEndpoint<Boolean>> endpoints = new LinkedHashSet<>();
            for (WireSystem.WireElement element : wireSystem.elements) {
                if (element.type == WireSystem.WireElement.Type.EMITTER_SIDE) {
                    endpoints.add(new SignalEndpoint<>(element.blockPos, element.emitterSide, type));
                } else if (element.type == WireSystem.WireElement.Type.WIRE_PART) {
                    for (Direction side : Direction.values()) {
                        endpoints.add(new SignalEndpoint<>(element.blockPos, side, type));
                    }
                }
            }
            result.add(new ClassicWireNetwork(type, Boolean.TRUE.equals(powered), List.copyOf(endpoints)));
        });
        return List.copyOf(result);
    }

    private record ClassicWireNetwork(
        SignalChannelType<Boolean> channel,
        Boolean value,
        Collection<SignalEndpoint<Boolean>> endpoints
    ) implements SignalNetworkView<Boolean> {}

    private static final class ClassicWirePort implements SignalPort<Boolean> {
        private final WireManager manager;
        private final Direction side;
        private final DyeColor color;
        private final SignalChannelType<Boolean> channel;

        private ClassicWirePort(WireManager manager, Direction side, DyeColor color, SignalChannelType<Boolean> channel) {
            this.manager = manager;
            this.side = side;
            this.color = color;
            this.channel = channel;
        }

        @Override public SignalChannelType<Boolean> channel() { return channel; }
        @Override public boolean connected() { return manager.hasPartOfColor(color); }
        @Override public Boolean value() { return connected() && manager.isAnyPowered(color); }

        @Override
        public SignalUpdateResult<Boolean> publish(Boolean value, OperationMode mode) {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(mode, "mode");
            boolean previous = manager.isSignalOutputActive(side, color);
            if (mode == OperationMode.EXECUTE && !manager.getHolder().getPipeWorld().isClientSide()) {
                manager.setSignalOutput(side, color, value);
            }
            return new SignalUpdateResult<>(previous != value, previous, value);
        }
    }
}
