/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.tile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import buildcraft.api.BCModules;
import buildcraft.api.core.EnumPipePart;
import buildcraft.api.core.InvalidInputDataException;
import buildcraft.api.tiles.IDebuggable;
import buildcraft.api.transport.pipe.IFlowItems;
import buildcraft.api.transport.pipe.IItemPipe;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pipe.PipeDefinition;
import buildcraft.api.transport.pipe.PipeEvent;
import buildcraft.api.transport.pipe.PipeEventTileState;
import buildcraft.api.transport.pipe.PipeFlow;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.compat.CompatCapTransfromer;
import buildcraft.lib.misc.AdvancementUtil;
import buildcraft.lib.misc.data.IdAllocator;
import buildcraft.lib.tile.TileBC_Neptune;
import buildcraft.silicon.plug.FilterEventHandler;
import buildcraft.transport.item.ItemPipeHolder;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.client.model.ModelPipe;
import buildcraft.transport.pipe.Pipe;
import buildcraft.transport.pipe.PipeEventBus;
import buildcraft.transport.pipe.PluggableHolder;
import buildcraft.transport.wire.WireManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class TilePipeHolder extends TileBC_Neptune implements IPipeHolder, IDebuggable{

    protected static final IdAllocator IDS = TileBC_Neptune.IDS.makeChild("pipe");

    public static final int NET_UPDATE_MULTI = IDS.allocId("UPDATE_MULTI");
    public static final int NET_UPDATE_PIPE_BEHAVIOUR = getReceiverId(PipeMessageReceiver.BEHAVIOUR);
    public static final int NET_UPDATE_PIPE_FLOW = getReceiverId(PipeMessageReceiver.FLOW);
    public static final int NET_UPDATE_PLUG_DOWN = getReceiverId(PipeMessageReceiver.PLUGGABLE_DOWN);
    public static final int NET_UPDATE_PLUG_UP = getReceiverId(PipeMessageReceiver.PLUGGABLE_UP);
    public static final int NET_UPDATE_PLUG_NORTH = getReceiverId(PipeMessageReceiver.PLUGGABLE_NORTH);
    public static final int NET_UPDATE_PLUG_SOUTH = getReceiverId(PipeMessageReceiver.PLUGGABLE_SOUTH);
    public static final int NET_UPDATE_PLUG_WEST = getReceiverId(PipeMessageReceiver.PLUGGABLE_WEST);
    public static final int NET_UPDATE_PLUG_EAST = getReceiverId(PipeMessageReceiver.PLUGGABLE_EAST);
    public static final int NET_UPDATE_WIRES = getReceiverId(PipeMessageReceiver.WIRES);

    private static final ResourceLocation ADVANCEMENT_PLACE_PIPE = ResourceLocation.parse(
        "buildcrafttransport:pipe_dream"
    );

    @Override
    public IdAllocator getIdAllocator() {
        return IDS;
    }

    private int[] redstoneValues = new int[6];
    private int[] oldRedstoneValues = new int[] { -1, -1, -1, -1, -1, -1 };

    static {
        for (PipeMessageReceiver rec : PipeMessageReceiver.values()) {
            IDS.allocId("UPDATE_" + rec);
        }
    }

    public static final int[] NET_UPDATE_PLUGS = { //
        NET_UPDATE_PLUG_DOWN, NET_UPDATE_PLUG_UP, //
        NET_UPDATE_PLUG_NORTH, NET_UPDATE_PLUG_SOUTH, //
        NET_UPDATE_PLUG_WEST, NET_UPDATE_PLUG_EAST,//
    };

    private static int getReceiverId(PipeMessageReceiver type) {
        return NET_UPDATE_MULTI + 1 + type.ordinal();
    }

    public final WireManager wireManager = new WireManager(this);
    public final PipeEventBus eventBus = new PipeEventBus();
    private final Map<Direction, PluggableHolder> pluggables = new EnumMap<>(Direction.class);
    private Pipe pipe = Pipe.EMPTY;
    private boolean scheduleRenderUpdate = true;
    private long lastPeriodicSaveTick = Long.MIN_VALUE;
    private boolean wasFlowPersistentlyActive;
    private final Set<PipeMessageReceiver> networkUpdates = EnumSet.noneOf(PipeMessageReceiver.class);
    private final Set<PipeMessageReceiver> networkGuiUpdates = EnumSet.noneOf(PipeMessageReceiver.class);
    
    protected ModelData modeldata;
    private CompoundTag unknownData;
    private HolderLookup.Provider serializationRegistries;
    public int hitPart = 0;

    public TilePipeHolder(BlockPos pos, BlockState bs) {
    	super(BCTransportBlocks.PIPE_HOLDER_BE.get(), pos, bs);
        for (Direction side : Direction.values()) {
            pluggables.put(side, new PluggableHolder(this, side));
        }
        caps.addCapabilityInstance(PipeApi.CAP_PIPE_HOLDER, this, EnumPipePart.values());
        caps.addCapability(PipeApi.CAP_PIPE, this::getPipe, EnumPipePart.values());
        caps.addCapability(PipeApi.CAP_PLUG, this::getPluggable, EnumPipePart.FACES);
    }

    
    
    // Read + write


	@Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        HolderLookup.Provider previousRegistries = serializationRegistries;
        serializationRegistries = registries;
        try {
            super.saveAdditional(nbt, registries);
            if (pipe != Pipe.EMPTY) {
                nbt.put("pipe", pipe.writeToNbt());
            } else if (unknownData != null) {
                // Never erase a pipe merely because this version cannot decode it yet.
                nbt.put("pipe", unknownData.copy());
            }
            CompoundTag plugs = new CompoundTag();
            for (Direction face : Direction.values()) {
                CompoundTag plugTag = pluggables.get(face).writeToNbt();
                if (!plugTag.isEmpty()) {
                    plugs.put(face.getName(), plugTag);
                }
            }
            if (!plugs.isEmpty()) {
                nbt.put("plugs", plugs);
            }
            nbt.put("wireManager", wireManager.writeToNbt());
            nbt.putIntArray("redstone", redstoneValues);
        } finally {
            serializationRegistries = previousRegistries;
        }
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        HolderLookup.Provider previousRegistries = serializationRegistries;
        serializationRegistries = registries;
        try {
            super.loadAdditional(nbt, registries);
            unknownData = null;
            pipe = Pipe.EMPTY;
            if (nbt.contains("pipe", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                CompoundTag pipeData = nbt.getCompound("pipe");
                try {
                    pipe = new Pipe(this, pipeData);
                    eventBus.registerHandler(pipe.behaviour);
                    eventBus.registerHandler(pipe.flow);
                    if (pipe.flow instanceof IFlowItems && BCModules.SILICON.isLoaded()) {
                        eventBus.registerHandler(FilterEventHandler.class);
                    }
                } catch (InvalidInputDataException | RuntimeException e) {
                    // A broken custom payload must not make Minecraft discard the complete
                    // block entity (pipe, facade, gate and wire data). Keep the raw pipe NBT.
                    buildcraft.api.core.BCLog.logger.warn(
                        "Failed to migrate pipe at {}; preserving its original NBT", worldPosition, e
                    );
                    pipe = Pipe.EMPTY;
                    unknownData = pipeData.copy();
                }
            }
            CompoundTag plugs = nbt.getCompound("plugs");
            for (Direction face : Direction.values()) {
                pluggables.get(face).readFromNbt(plugs.getCompound(face.getName()));
            }
            wireManager.readFromNbt(nbt.getCompound("wireManager"));
            if (nbt.contains("redstone")) {
                int[] temp = nbt.getIntArray("redstone");
                if (temp.length == 6) {
                    redstoneValues = temp;
                }
            }
        } finally {
            serializationRegistries = previousRegistries;
        }
    }

    public static HolderLookup.Provider getRegistryAccess(IPipe pipe) {
        if (pipe.getHolder().getPipeTile() instanceof TilePipeHolder holder
            && holder.serializationRegistries != null) {
            return holder.serializationRegistries;
        }
        Level world = pipe.getHolder().getPipeWorld();
        if (world != null) {
            return world.registryAccess();
        }
        throw new IllegalStateException("Cannot serialize pipe data without a registry lookup");
    }
    
    

    // Misc

	@Override
    public void onPlacedBy(LivingEntity placer, ItemStack stack) {
        super.onPlacedBy(placer, stack);
        Item item = stack.getItem();
        if (item instanceof IItemPipe) {
            PipeDefinition definition = ((IItemPipe) item).getDefinition();
            this.unknownData = null;
            this.pipe = new Pipe(this, definition);
            unknownData = null;
            eventBus.registerHandler(pipe.behaviour);
            eventBus.registerHandler(pipe.flow);
            if (pipe.flow instanceof IFlowItems && BCModules.SILICON.isLoaded()) {
                eventBus.registerHandler(FilterEventHandler.class);
            }
            int meta = ItemPipeHolder.getPipeColorId(stack);
            if (meta > 0 && meta <= 16) {
                pipe.setColour(DyeColor.byId(meta - 1));
            }
        }
        scheduleRenderUpdate();

        if (!level.isClientSide && hasOwner()) {
            AdvancementUtil.unlockAdvancement(getOwner().getId(), ADVANCEMENT_PLACE_PIPE);
        }
    }

	@Override
    public void onRemove(boolean dropSelf) {
        super.onRemove(dropSelf);
        if (level != null && !level.isClientSide) {
            for (Direction face : Direction.values()) {
                PipePluggable pluggable = getPluggable(face);
                if (pluggable != PipePluggable.EMPTY) {
                    pluggable.onRemove();
                }
            }
        }
    }

	@Override
	public void setRemoved() {
		super.setRemoved();
		eventBus.fireEvent(new PipeEventTileState.Invalidate(this));
		wireManager.invalidate();
	}

	@Override
    public void clearRemoved() {
        super.clearRemoved();
        eventBus.fireEvent(new PipeEventTileState.Validate(this));
        wireManager.validate();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        eventBus.fireEvent(new PipeEventTileState.ChunkUnload(this));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (pipe != Pipe.EMPTY) {
            pipe.onLoad();
        }
        wireManager.validate();
    }
    
    @Override
	public void neighbourBlockChanged(BlockState state, BlockPos neighbor, boolean harvest) {
        if (level.isClientSide()) {
            return;
        }
        if (pipe != Pipe.EMPTY) {
            pipe.markForUpdate();
        }
	}

	@Override
    public void onNeighbourBlockChanged(BlockState state, BlockPos neighbour) {

    }
    
    // ITickable

    public void update() {
        redstoneValues = new int[6];
        // Tick objects
        if (pipe != Pipe.EMPTY) {
            pipe.onTick();
        }
        for (Direction face : Direction.values()) {
            pluggables.get(face).onTick();
        }

        // Send network updates
        if (networkUpdates.size() > 0) {
            // TODO: Multi-update messages! (multiple updates sent in a single message)
            Set<PipeMessageReceiver> parts = EnumSet.copyOf(networkUpdates);
            for (PipeMessageReceiver part : parts) {
                sendNetworkUpdate(getReceiverId(part));
            }
        }
        // No need to send gui updates to specific players if we just sent off messages to all players.
        networkGuiUpdates.removeAll(networkUpdates);
        networkUpdates.clear();

        if (networkGuiUpdates.size() > 0) {
            // TODO: Multi-update messages! (multiple updates sent in a single message)
            Set<PipeMessageReceiver> parts = EnumSet.copyOf(networkGuiUpdates);
            for (PipeMessageReceiver part : parts) {
                sendNetworkGuiUpdate(getReceiverId(part));
            }
        }
        networkGuiUpdates.clear();

        if (scheduleRenderUpdate) {
            scheduleRenderUpdate = false;
            redrawBlock();
        }

        wireManager.tick();

        if (!Arrays.equals(redstoneValues, oldRedstoneValues)) {
            Block block = level.getBlockState(worldPosition).getBlock();
            level.updateNeighborsAt(worldPosition, block);
            for (int i = 0; i < 6; i++) {
                Direction face = Direction.values()[i];
                if (oldRedstoneValues[i] != redstoneValues[i]) {
                    level.updateNeighborsAt(worldPosition.offset(face.getNormal()), block);
                }
            }
            oldRedstoneValues = redstoneValues;
        }

        if (!level.isClientSide && pipe != Pipe.EMPTY) {
            boolean flowActive = pipe.flow.requiresPeriodicSave();
            long now = level.getGameTime();
            if (flowActive && (lastPeriodicSaveTick == Long.MIN_VALUE || now - lastPeriodicSaveTick >= 20)) {
                markChunkDirty();
                lastPeriodicSaveTick = now;
            } else if (!flowActive && wasFlowPersistentlyActive) {
                // Persist the transition to empty as well, otherwise the last saved travelling item/fluid could
                // reappear after a reload.
                markChunkDirty();
                lastPeriodicSaveTick = now;
            }
            wasFlowPersistentlyActive = flowActive;
        }
    }

    // Network

    @Override
    public void writePayload(int id, FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(id, buffer, side);
        if (side == LogicalSide.SERVER) {
            if (id == NET_RENDER_DATA) {
                if (pipe == Pipe.EMPTY) {
                    buffer.writeBoolean(false);
                } else {
                    buffer.writeBoolean(true);
                    pipe.writeCreationPayload(buffer);
                }
                for (Direction face : Direction.values()) {
                    pluggables.get(face).writeCreationPayload(buffer);
                }
                wireManager.writePayload(buffer, side);
            } else if (id == NET_UPDATE_PIPE_BEHAVIOUR) {
                if (pipe == Pipe.EMPTY) {
                    buffer.writeBoolean(false);
                } else {
                    buffer.writeBoolean(true);
                    pipe.writePayload(buffer, side);
                }
            } else if (id == NET_UPDATE_WIRES) {
                wireManager.writePayload(buffer, side);
            }
        }
        if (id == NET_UPDATE_PIPE_FLOW) {
            if (pipe == Pipe.EMPTY || pipe.flow == null) {
                buffer.writeBoolean(false);
            } else {
                buffer.writeBoolean(true);
                pipe.flow.writePayload(PipeFlow.NET_ID_UPDATE, buffer, side);
            }
        } else if (id == NET_UPDATE_PLUG_DOWN) pluggables.get(Direction.DOWN).writePayload(buffer, side);
        else if (id == NET_UPDATE_PLUG_UP) pluggables.get(Direction.UP).writePayload(buffer, side);
        else if (id == NET_UPDATE_PLUG_NORTH) pluggables.get(Direction.NORTH).writePayload(buffer, side);
        else if (id == NET_UPDATE_PLUG_SOUTH) pluggables.get(Direction.SOUTH).writePayload(buffer, side);
        else if (id == NET_UPDATE_PLUG_WEST) pluggables.get(Direction.WEST).writePayload(buffer, side);
        else if (id == NET_UPDATE_PLUG_EAST) pluggables.get(Direction.EAST).writePayload(buffer, side);
    }

    @Override
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side, IPayloadContext ctx) throws IOException {
        super.readPayload(id, buffer, side, ctx);
        if (side == LogicalSide.CLIENT) {
            if (id == NET_RENDER_DATA) {
            	if (buffer.readBoolean()) {
                    pipe = new Pipe(this, buffer, ctx);
                    eventBus.registerHandler(pipe.behaviour);
                    eventBus.registerHandler(pipe.flow);
                    if (pipe.flow instanceof IFlowItems && BCModules.SILICON.isLoaded()) {
                        eventBus.registerHandler(FilterEventHandler.class);
                    }
                } else if (pipe != Pipe.EMPTY) {
                    eventBus.unregisterHandler(pipe.behaviour);
                    eventBus.unregisterHandler(pipe.flow);
                    pipe = Pipe.EMPTY;
                }
                for (Direction face : Direction.values()) {
                    pluggables.get(face).readCreationPayload(buffer);
                }
                wireManager.readPayload(buffer, side, ctx);
            } else if (id == NET_UPDATE_MULTI) {
                int total = buffer.readUnsignedByte();
                for (PipeMessageReceiver type : PipeMessageReceiver.values()) {
                    if (((total >> type.ordinal()) & 1) == 1) {
                        readPayload(getReceiverId(type), buffer, side, ctx);
                    }
                }
            } else if (id == NET_UPDATE_PIPE_BEHAVIOUR) {
            	requestModelDataUpdate();//DON'T WORK
            	level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
                if (buffer.readBoolean()) {
                    if (pipe == Pipe.EMPTY) {
                        throw new IllegalStateException("Pipe was null when it shouldn't have been!");
                    } else {
                        pipe.readPayload(buffer, side, ctx);
                    }
                }
            } else if (id == NET_UPDATE_WIRES) {
                wireManager.readPayload(buffer, side, ctx);
            }
        }
        if (id == NET_UPDATE_PIPE_FLOW) {
            if (buffer.readBoolean()) {
                if (pipe == Pipe.EMPTY) {
                    throw new IllegalStateException("Pipe was null when it shouldn't have been!");
                } else {
                    int fId = buffer.readShort();
                    pipe.flow.readPayload(fId, buffer, side);
                }
            }
        } else if (id == NET_UPDATE_PLUG_DOWN) pluggables.get(Direction.DOWN).readPayload(buffer, side, ctx);
        else if (id == NET_UPDATE_PLUG_UP) pluggables.get(Direction.UP).readPayload(buffer, side, ctx);
        else if (id == NET_UPDATE_PLUG_NORTH) pluggables.get(Direction.NORTH).readPayload(buffer, side, ctx);
        else if (id == NET_UPDATE_PLUG_SOUTH) pluggables.get(Direction.SOUTH).readPayload(buffer, side, ctx);
        else if (id == NET_UPDATE_PLUG_WEST) pluggables.get(Direction.WEST).readPayload(buffer, side, ctx);
        else if (id == NET_UPDATE_PLUG_EAST) pluggables.get(Direction.EAST).readPayload(buffer, side, ctx);
    }

    // IPipeHolder

    @Override
	public Level getPipeWorld() {
		return getLevel();
	}

    @Override
    public BlockPos getPipePos() {
        return getBlockPos();
    }


	@Override
    public BlockEntity getPipeTile() {
        return this;
    }

    @Override
    public Pipe getPipe() {
        return pipe;
    }

    @Override
    public boolean canPlayerInteract(Player player) {
        return canInteractWith(player);
    }

    @Override
    public PipePluggable getPluggable(Direction side) {
        if (side == null) return PipePluggable.EMPTY;
        return pluggables.get(side).pluggable;
    }

    public PipePluggable replacePluggable(Direction side, PipePluggable with) {
        redstoneValues = new int[6];
        PluggableHolder holder = pluggables.get(side);
        PipePluggable old = holder.pluggable;
        holder.setPluggable(with);

        eventBus.unregisterHandler(old);
        eventBus.registerHandler(with);

        if (pipe != Pipe.EMPTY) {
            pipe.markForUpdate();
        }
        if (!level.isClientSide()) {
            if (old != with) {
                wireManager.getWireSystems().rebuildWireSystemsAround(this);
                markChunkDirty();
            }
            holder.sendNewPluggableData();
        }
        scheduleRenderUpdate();
        level.neighborChanged(worldPosition.offset(side.getNormal()), BCTransportBlocks.pipeHolder.get(), worldPosition);
        return old;
    }

    @Override
    public IPipe getNeighbourPipe(Direction side) {
        BlockEntity neighbour = getNeighbourTile(side);
        if (neighbour == null || neighbour.getLevel() == null) {
            return Pipe.EMPTY;
        }
        IPipe neighbourPipe = neighbour.getLevel().getCapability(
            PipeApi.CAP_PIPE, neighbour.getBlockPos(), side.getOpposite()
        );
        return neighbourPipe == null ? Pipe.EMPTY : neighbourPipe;
    }

    
    
    @Override
    @Nullable
    public <T> T getCapabilityFromPipe(
        Direction side, @Nonnull BlockCapability<T, Direction> capability
    ) {
        PipePluggable plug = getPluggable(side);
        if (plug != PipePluggable.EMPTY) {
            T value = plug.getInternalCapability(capability);
            if (value != null) {
                return value;
            }
            if (plug.isBlocking()) {
                return null;
            }
        }
        if (pipe.isConnected(side)) {
            BlockEntity neighbour = getNeighbourTile(side);
            if (neighbour != null) {
                return CompatCapTransfromer.INSTANCE
                    .getCap(neighbour, capability, side.getOpposite())
                    .orElse(null);
            }
        }
        return null;
    }

    @Override
    public void scheduleRenderUpdate() {
        scheduleRenderUpdate = true;
    }

    @Override
    public void scheduleNetworkUpdate(PipeMessageReceiver... parts) {
        Collections.addAll(networkUpdates, parts);
        if (level != null && !level.isClientSide) {
            markChunkDirty();
        }
    }

    @Override
    public void scheduleNetworkGuiUpdate(PipeMessageReceiver... parts) {
        Collections.addAll(networkGuiUpdates, parts);
    }

    @Override
    public void sendMessage(PipeMessageReceiver to, IWriter writer) {
        createAndSendMessage(getReceiverId(to), writer::write);
    }

    @Override
    public void sendGuiMessage(PipeMessageReceiver to, IWriter writer) {
        createAndSendGuiMessage(getReceiverId(to), writer::write);
    }

    @Override
    public WireManager getWireManager() {
        return wireManager;
    }

    @Override
    public boolean fireEvent(PipeEvent event) {
        return eventBus.fireEvent(event);
    }

    @Override
    public int getRedstoneInput(Direction side) {
        if (side == null) {
            return level.getBestNeighborSignal(worldPosition);
        } else {
            return level.getSignal(worldPosition.offset(side.getNormal()), side);
        }
    }

    @Override
    public boolean setRedstoneOutput(Direction side, int value) {
        if (side == null) {
            for (Direction facing : Direction.values()) {
                redstoneValues[facing.ordinal()] = value;
            }
        } else {
            redstoneValues[side.ordinal()] = value;
        }
        return true;
    }

    public int getRedstoneOutput(Direction side) {
        return redstoneValues[side.ordinal()];
    }
    
    @Override
	public void rotate(Rotation axis) {
    	if(axis == Rotation.NONE)
    		return;
    	Map<Direction, PluggableHolder> copyPluggables = Map.copyOf(pluggables);
    	pluggables.clear();
        for (Map.Entry<Direction, PluggableHolder> e : copyPluggables.entrySet()) {
            PluggableHolder plug = e.getValue();
			plug.rotate(axis);
            pluggables.put(axis.rotate(e.getKey()), plug);
        }

        wireManager.rotate(axis);
        int[] newRedstione = new int[6];
        for(Direction face : Direction.values())
        	newRedstione[axis.rotate(face).ordinal()] = redstoneValues[face.ordinal()];
        redstoneValues = newRedstione;
        pipe.rotate(axis);
        if (level != null && !level.isClientSide) {
            markChunkDirty();
        }
	}

    // Caps

    
    @Override
    @Nullable
    public <T> T getCapability(
        BlockCapability<T, Direction> capability, @Nullable Direction facing
    ) {
        if (facing != null) {
            PipePluggable plug = getPluggable(facing);
            if (plug != PipePluggable.EMPTY) {
                T value = plug.getCapability(capability);
                if (value != null) {
                    return value;
                }
                if (plug.isBlocking()) {
                    return null;
                }
            }
        }
        if (pipe != Pipe.EMPTY) {
            T value = pipe.getCapability(capability, facing);
            if (value != null) {
                return value;
            }
        }
        return super.getCapability(capability, facing);
    }

    
	@Override
	public @NotNull ModelData getModelData() {
		if (modeldata == null) {
			modeldata = TilePipeHolderModelData.build(this);
		}
		return modeldata;
	}

    // Client side stuffs

    @Override
    public void getDebugInfo(List<String> left, List<String> right, Direction side) {
        if (pipe == Pipe.EMPTY) {
            left.add("Pipe = null");
        } else {
            left.add("Pipe:");
            pipe.getDebugInfo(left, right, side);
        }
        left.add("Parts:");
        wireManager.parts
            .forEach((part, color) -> left.add(" - " + part + " = " + color + " = " + wireManager.isPowered(part)));
        left.add("All wire systems in world count = "
            + (level.isClientSide ? 0 : wireManager.getWireSystems().wireSystems.size()));
        if (unknownData != null) {
            left.add(unknownData.toString());
        }
    }

}
