package buildcraft.compat.forestry.pipe;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import buildcraft.api.core.BCLog;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.compat.BuildCraftCompat;
import forestry.api.IForestryApi;
import forestry.api.genetics.ISpecies;
import forestry.core.utils.SpeciesUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Dedicated packets for Forestry's genetic filter UI hosted by a BuildCraft pipe. */
public final class ForestryPropolisNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(BuildCraftCompat.MODID, "forestry_propolis"),
        () -> PROTOCOL,
        PROTOCOL::equals,
        PROTOCOL::equals
    );
    private static int nextId;
    private static boolean registered;

    private ForestryPropolisNetwork() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;

        CHANNEL.messageBuilder(RuleChange.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(RuleChange::encode)
            .decoder(RuleChange::decode)
            .consumerMainThread(ForestryPropolisNetwork::handleRuleChange)
            .add();
        CHANNEL.messageBuilder(GenomeChange.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(GenomeChange::encode)
            .decoder(GenomeChange::decode)
            .consumerMainThread(ForestryPropolisNetwork::handleGenomeChange)
            .add();
        CHANNEL.messageBuilder(FilterState.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(FilterState::encode)
            .decoder(FilterState::decode)
            .consumerMainThread(ForestryPropolisNetwork::handleFilterState)
            .add();
    }

    public static void sendRuleChange(BlockPos pos, Direction facing, String ruleId) {
        CHANNEL.sendToServer(new RuleChange(pos, facing, ruleId));
    }

    public static void sendGenomeChange(BlockPos pos, Direction facing, int index, boolean active,
            @Nullable ResourceLocation speciesId) {
        CHANNEL.sendToServer(new GenomeChange(pos, facing, index, active, speciesId));
    }

    public static void sendStateToViewers(ServerLevel level, BlockPos pos, PropolisFilterLogic logic) {
        FilterState state = new FilterState(pos, logic.write(new CompoundTag()));
        for (ServerPlayer player : level.players()) {
            if (player.containerMenu instanceof ContainerPropolisPipe menu && menu.hasSamePipe(pos)) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), state);
            }
        }
    }

    @Nullable
    public static PipeBehaviourPropolis findBehaviour(Level level, BlockPos pos) {
        BlockEntity tile = level.getBlockEntity(pos);
        if (tile instanceof IPipeHolder holder
                && holder.getPipe() != null
                && holder.getPipe().getBehaviour() instanceof PipeBehaviourPropolis behaviour) {
            return behaviour;
        }
        return null;
    }

    private static boolean canEdit(ServerPlayer player, BlockPos pos) {
        return player.containerMenu instanceof ContainerPropolisPipe menu
            && menu.hasSamePipe(pos)
            && menu.stillValid(player);
    }

    private static void handleRuleChange(RuleChange message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player == null || !canEdit(player, message.pos)) {
            return;
        }
        PipeBehaviourPropolis behaviour = findBehaviour(player.level(), message.pos);
        if (behaviour == null) {
            return;
        }
        var rule = IForestryApi.INSTANCE.getFilterManager().getRuleOrDefault(message.ruleId);
        if (behaviour.getFilter().setRule(message.facing, rule)) {
            behaviour.filterChanged(player);
        }
    }

    private static void handleGenomeChange(GenomeChange message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player == null || message.index < 0 || message.index >= 3 || !canEdit(player, message.pos)) {
            return;
        }
        PipeBehaviourPropolis behaviour = findBehaviour(player.level(), message.pos);
        if (behaviour == null) {
            return;
        }
        ISpecies<?> species = message.speciesId == null ? null : SpeciesUtil.getAnySpecies(message.speciesId);
        if (behaviour.getFilter().setGenomeFilter(message.facing, message.index, message.active, species)) {
            behaviour.filterChanged(player);
        }
    }

    private static void handleFilterState(FilterState message, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> invokeClientFilterState(message.pos, message.filter));
    }

    private static void invokeClientFilterState(BlockPos pos, CompoundTag filter) {
        try {
            Class.forName("buildcraft.compat.forestry.pipe.client.ForestryCompatClient")
                .getMethod("handleFilterState", BlockPos.class, CompoundTag.class)
                .invoke(null, pos, filter);
        } catch (ReflectiveOperationException | LinkageError e) {
            BCLog.logger.error("Failed to apply Apiarist's Pipe filter state on the client", e);
        }
    }

    private record RuleChange(BlockPos pos, Direction facing, String ruleId) {
        private static void encode(RuleChange message, FriendlyByteBuf buffer) {
            buffer.writeBlockPos(message.pos);
            buffer.writeByte(message.facing.ordinal());
            buffer.writeUtf(message.ruleId);
        }

        private static RuleChange decode(FriendlyByteBuf buffer) {
            return new RuleChange(buffer.readBlockPos(), Direction.from3DDataValue(buffer.readUnsignedByte()),
                buffer.readUtf());
        }
    }

    private record GenomeChange(BlockPos pos, Direction facing, int index, boolean active,
            @Nullable ResourceLocation speciesId) {
        private static void encode(GenomeChange message, FriendlyByteBuf buffer) {
            buffer.writeBlockPos(message.pos);
            buffer.writeByte(message.facing.ordinal());
            buffer.writeByte(message.index);
            buffer.writeBoolean(message.active);
            buffer.writeBoolean(message.speciesId != null);
            if (message.speciesId != null) {
                buffer.writeResourceLocation(message.speciesId);
            }
        }

        private static GenomeChange decode(FriendlyByteBuf buffer) {
            BlockPos pos = buffer.readBlockPos();
            Direction facing = Direction.from3DDataValue(buffer.readUnsignedByte());
            int index = buffer.readUnsignedByte();
            boolean active = buffer.readBoolean();
            ResourceLocation speciesId = buffer.readBoolean() ? buffer.readResourceLocation() : null;
            return new GenomeChange(pos, facing, index, active, speciesId);
        }
    }

    private record FilterState(BlockPos pos, CompoundTag filter) {
        private static void encode(FilterState message, FriendlyByteBuf buffer) {
            buffer.writeBlockPos(message.pos);
            buffer.writeNbt(message.filter);
        }

        private static FilterState decode(FriendlyByteBuf buffer) {
            BlockPos pos = buffer.readBlockPos();
            CompoundTag filter = buffer.readNbt();
            return new FilterState(pos, filter == null ? new CompoundTag() : filter);
        }
    }
}
